package de.hf.myfinance.marketdata.importhandler;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.marketdata.persistence.DataReaderImpl;
import de.hf.myfinance.marketdata.persistence.KeyTsProjection;
import de.hf.myfinance.marketdata.webtools.WebRequest;
import de.hf.myfinance.restmodel.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AlphavantageHandler implements ImportHandler {

    private final DataReaderImpl dataReaderImpl;

    WebRequest webRequest;
    AuditService auditService;

    public final static String ALPHAVANTAGE_API_KEY = "Q6RLS6PGB55105EP"; // Replace with your actual API key
    public final static String URLPREFIX = "https://www.alphavantage.co/query?function=";
    public final static String SYMBOL_PREFIX = "&symbol=";

    public final static String EQ_FUNCTION = "TIME_SERIES_WEEKLY";
    public final static String EQ_URLPREFIX = URLPREFIX + EQ_FUNCTION + SYMBOL_PREFIX;
    public final static String EQ_URLPOSTFIX = "&apikey=" + ALPHAVANTAGE_API_KEY;

    public final static String FX_FUNCTION = "FX_DAILY";
    public final static String FX_URLPREFIX = URLPREFIX+FX_FUNCTION+"&from_symbol=";
    public final static String FX_URLPOSTFIX = "&to_symbol=EUR&apikey=" + ALPHAVANTAGE_API_KEY;

    public final static String SECURITYMETRICS_OVERVIEW_FUNCTION = "OVERVIEW";
    public final static String SECURITYMETRICS_OVERVIEW_URLPREFIX = URLPREFIX+SECURITYMETRICS_OVERVIEW_FUNCTION+SYMBOL_PREFIX;
    public final static String SECURITYMETRICS_OVERVIEW_URLPOSTFIX = "&apikey=" + ALPHAVANTAGE_API_KEY;

    private static final Integer NUMBER_OF_INSTRUMENT2IMPORT = 5;

    protected static final String AUDIT_MSG_TYPE = "AlphavantageHandler_Event";

    public AlphavantageHandler(WebRequest webRequest, AuditService auditService, DataReaderImpl dataReaderImpl) {
        this.webRequest = webRequest;
        this.auditService = auditService;
        this.dataReaderImpl = dataReaderImpl;
    }

    public Map<LocalDate, EndOfDayPrice> importPrices(Instrument security) {
        final Map<LocalDate, EndOfDayPrice> values = new HashMap<>();
        InstrumentType securityType = security.getInstrumentType();
        if (securityType.equals(InstrumentType.EQUITY)) {
            var symbols = security.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS);
            if (symbols != null && !symbols.isEmpty()) {
                symbols.keySet().forEach(s -> {
                    String url = EQ_URLPREFIX + s + EQ_URLPOSTFIX;
                    var currency = symbols.get(s);
                    var prices = getTimeSeries( "Weekly Time Series", url);
                    add2Pricemap(values, currency, prices);
                });
            }
        } else if (securityType.equals(InstrumentType.CURRENCY)) {
            String currencyCode = security.getAdditionalProperties().get(AdditionalProperties.CURRENCYCODE);
            String url = FX_URLPREFIX + currencyCode + FX_URLPOSTFIX;
            var prices = getTimeSeries( "Time Series FX (Daily)", url);
            add2Pricemap(values, Instrument.DEFAULTCURRENCY, prices);
        }

        return values;
    }

    private Map<LocalDate, EndOfDayPrice> add2Pricemap(Map<LocalDate, EndOfDayPrice> values, String currency,
            Map<LocalDate, Double> prices) {
        prices.entrySet().forEach(entry -> {
            var endOfDayPrice = new EndOfDayPrice(entry.getValue(), currency);
            values.put(entry.getKey(), endOfDayPrice);
        });
        return values;
    }

    private Map<LocalDate, Double> getTimeSeries(String timeSeriesName, String url) {
        Map<LocalDate, Double> prices = new HashMap<>();
        Map<String, Object> map = webRequest.getJsonMapFromUrl(url);
        Map<String, Object> timeSeries = (Map<String, Object>) map.get(timeSeriesName);
        if (timeSeries == null) {
            auditService.saveMessage("invalid request or no data for url " + url, Severity.ERROR, AUDIT_MSG_TYPE);
            if (map != null && map.containsKey("Error Message")) {
                auditService.saveMessage("Alphavantage report an error: " + map.get("Error Message"), Severity.ERROR,
                        AUDIT_MSG_TYPE);
            }
        } else {
            for (String dateString : timeSeries.keySet()) {
                try {
                    LocalDate date = LocalDate.parse(dateString);
                    String valueString = ((Map<String, String>) timeSeries.get(dateString)).get("4. close");
                    Double value = Double.parseDouble(valueString);

                    prices.put(date, value);

                } catch (Exception e) {
                    auditService.saveMessage("can not parse value for date" + dateString + " and url " + url,
                            Severity.ERROR, AUDIT_MSG_TYPE);
                }
            }
        }
        return prices;
    }

    @Override
    /**
     * filter active instruments with symbol and take 5 instruments with no 5 with
     * oldest prices
     */
    public Mono<List<Instrument>> filterInstruments(List<Instrument> instruments, Flux<KeyTsProjection> keyTsFlux) {
        List<Instrument> relevantInstruments = instruments.stream()
                .filter(i -> i.getInstrumentType().equals(InstrumentType.CURRENCY)
                        || (i.getAdditionalMaps() != null &&
                                i.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS) != null))
                .collect(Collectors.toList());

        return keyTsFlux
                .collectMap(KeyTsProjection::getInstrumentBusinesskey, KeyTsProjection::getLastUpdateTs)
                .map(keyTsMap -> {
                    // 1. Instruments NOT in KeyTsProjection
                    List<Instrument> nonMatching = relevantInstruments.stream()
                            .filter(instr -> !keyTsMap.containsKey(instr.getBusinesskey()))
                            .limit(NUMBER_OF_INSTRUMENT2IMPORT)
                            .collect(Collectors.toList());

                    // 2. Instruments that ARE in KeyTsProjection
                    List<Instrument> matchingSortedByOldestTs = relevantInstruments.stream()
                            .filter(instr -> keyTsMap.containsKey(instr.getBusinesskey()))
                            .sorted(Comparator.comparing(
                                    instr -> {
                                        LocalDateTime ts = keyTsMap.get(instr.getBusinesskey());
                                        return ts != null ? ts : LocalDateTime.MIN;
                                    }))
                            .limit(NUMBER_OF_INSTRUMENT2IMPORT)
                            .collect(Collectors.toList());

                    // 3. Combine results
                    List<Instrument> combined = new ArrayList<>(nonMatching);
                    combined.addAll(matchingSortedByOldestTs);
                    return combined;
                });
    }

    @Override
    public SecurityMetrics importSecurityMetrics(Instrument security) {
        var securityMetrics = new SecurityMetrics();
        securityMetrics.setBusinesskey(security.getBusinesskey());
        securityMetrics.setDescription(security.getDescription());
        InstrumentType securityType = security.getInstrumentType();
        if (securityType.equals(InstrumentType.EQUITY)) {
            var symbols = security.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS);
            if (symbols != null && !symbols.isEmpty()) {

                String symbol = symbols.keySet().stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("No symbol found for security: " + security.getBusinesskey()));
                 
                String url = SECURITYMETRICS_OVERVIEW_URLPREFIX + symbol + SECURITYMETRICS_OVERVIEW_URLPOSTFIX;
                Map<String, Object> map = webRequest.getJsonMapFromUrl(url);
                if (map != null && !map.isEmpty()) {
                        securityMetrics.setCurrencyCode(map.get("Currency").toString());
                        securityMetrics.setSector(map.get("Sector").toString());
                        securityMetrics.setDividendPerShare(Double.valueOf(map.get("DividendPerShare").toString()));
                        securityMetrics.setEps(Double.valueOf(map.get("EPS").toString()));
                        securityMetrics.setSharesOutstanding(Double.valueOf(map.get("SharesOutstanding").toString()));
                        securityMetrics.setRevenue(Double.valueOf(map.get("RevenueTTM").toString()));
                        securityMetrics.setBeta(Double.valueOf(map.get("Beta").toString()));
                }
            }
        }

        return securityMetrics;
    }

}
