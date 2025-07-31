package de.hf.myfinance.marketdata.importhandler;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.marketdata.persistence.DataReaderImpl;
import de.hf.myfinance.marketdata.persistence.KeyTsProjection;
import de.hf.myfinance.marketdata.webtools.WebRequest;
import de.hf.myfinance.restmodel.*;
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

    public final static String EQ_URLPREFIX = "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol=";
    public final static String EQ_URLPOSTFIX = "&apikey=Q6RLS6PGB55105EP";
    public final static String FX_URLPREFIX = "https://www.alphavantage.co/query?function=FX_DAILY&from_symbol=";
    public final static String FX_URLPOSTFIX = "&to_symbol=EUR&apikey=Q6RLS6PGB55105EP";

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
                    var prices = getTimeSeries(security, "Weekly Time Series", url);
                    add2Pricemap(values, currency, prices);
                });
            }
        } else if (securityType.equals(InstrumentType.CURRENCY)) {
            String currencyCode = security.getAdditionalProperties().get(AdditionalProperties.CURRENCYCODE);
            String url = FX_URLPREFIX + currencyCode + FX_URLPOSTFIX;
            var prices = getTimeSeries(security, "Time Series FX (Daily)", url);
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

    private Map<LocalDate, Double> getTimeSeries(Instrument security, String timeSeriesName, String url) {
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
    public Mono<List<Instrument>> filterInstruments(List<Instrument> instruments) {
        List<Instrument> relevantInstruments = instruments.stream()
                .filter(i -> i.getInstrumentType().equals(InstrumentType.CURRENCY)
                        || (i.getAdditionalMaps() != null &&
                                i.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS) != null))
                .collect(Collectors.toList());

        return dataReaderImpl.getKeyToTsMap()
                .collectMap(KeyTsProjection::getInstrumentBusinesskey, KeyTsProjection::getLastUpdateTs)
                .map(keyTsMap -> {
                    // 1. Instruments NOT in KeyTsProjection
                    List<Instrument> nonMatching = relevantInstruments.stream()
                            .filter(instr -> !keyTsMap.containsKey(instr.getBusinesskey()))
                            .limit(5)
                            .collect(Collectors.toList());

                    // 2. Instruments that ARE in KeyTsProjection
                    List<Instrument> matchingSortedByOldestTs = relevantInstruments.stream()
                            .filter(instr -> keyTsMap.containsKey(instr.getBusinesskey()))
                            .sorted(Comparator.comparing(
                                    instr -> {
                                        LocalDateTime ts = keyTsMap.get(instr.getBusinesskey());
                                        return ts != null ? ts : LocalDateTime.MIN;
                                    }))
                            .limit(5)
                            .collect(Collectors.toList());

                    // 3. Combine results
                    List<Instrument> combined = new ArrayList<>(nonMatching);
                    combined.addAll(matchingSortedByOldestTs);
                    return combined;
                });
    }

}
