package de.hf.myfinance.marketdata.importhandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.marketdata.persistence.DataReaderImpl;
import de.hf.myfinance.marketdata.persistence.KeyTsProjection;
import de.hf.myfinance.marketdata.webtools.WebRequest;
import de.hf.myfinance.restmodel.AdditionalMaps;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.EndOfDayPrice;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.SecurityMetrics;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class PolygonHandler  implements ImportHandler {

    private final DataReaderImpl dataReaderImpl;

    WebRequest webRequest;
    AuditService auditService;

    //https://api.polygon.io/v2/aggs/ticker/SAP/prev?apiKey=bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p
    //https://api.polygon.io/v2/aggs/ticker/C:USDEUR/prev?apiKey=bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p
    //https://api.polygon.io/vX/reference/financials?ticker=MSFT&timeframe=ttm&order=asc&limit=10&sort=filing_date&apiKey=bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p

    public final static String API_KEY = "bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p"; // Replace with your actual API key
    public final static String URLPREFIX = "https://api.polygon.io/";
    public final static String PRICE_PREFIX = "v2/aggs/ticker/";

    public final static String URLPOSTFIX = "apiKey=" + API_KEY;

    public final static String PRICE_URLPOSTFIX = "/prev?" + URLPOSTFIX;
    public final static String PRICE_URLPREFIX = URLPREFIX + PRICE_PREFIX;
    public final static String FX_URLPREFIX = PRICE_URLPREFIX + "C:";
    public final static String SECURITYMETRICS_PREFIX = URLPREFIX + "vX/reference/financials?ticker=";
    public final static String SECURITYMETRICS_URLPOSTFIX = "&timeframe=ttm&order=asc&sort=filing_date&" + URLPOSTFIX;




    protected static final String AUDIT_MSG_TYPE = "PolygonHandler_Event";

    public PolygonHandler(WebRequest webRequest, AuditService auditService, DataReaderImpl dataReaderImpl) {
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
                    String url = PRICE_URLPREFIX + s + PRICE_URLPOSTFIX;
                    var currency = symbols.get(s);
                    var prices = getPreviosEndOfDayPrice(url);
                    add2Pricemap(values, currency, prices);
                });
            }
        } else if (securityType.equals(InstrumentType.CURRENCY)) {
            String currencyCode = security.getAdditionalProperties().get(AdditionalProperties.CURRENCYCODE);
            String url = FX_URLPREFIX + currencyCode + PRICE_URLPOSTFIX;
            var prices = getPreviosEndOfDayPrice(url);
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

    private Map<LocalDate, Double> getPreviosEndOfDayPrice(String url) {
        Map<LocalDate, Double> prices = new HashMap<>();
        Map<String, Object> map = webRequest.getJsonMapFromUrl(url);
        Map<String, Object> timeSeries = (Map<String, Object>) map.get("results");
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

        return Mono.just(relevantInstruments);
    }

    @Override
    /**
     * filter active instruments with symbol and take 5 instruments with no 5 with
     * oldest fundamentals
     */
    public Mono<List<Instrument>> filterEQInstruments(List<Instrument> instruments, Flux<KeyTsProjection> keyTsFlux) {
        List<Instrument> relevantInstruments = instruments.stream()
                .filter(i -> (i.getAdditionalMaps() != null &&
                        i.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS) != null))
                .collect(Collectors.toList());

       return Mono.just(relevantInstruments);
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
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No symbol found for security: " + security.getBusinesskey()));
                String url = SECURITYMETRICS_PREFIX + symbol + SECURITYMETRICS_URLPOSTFIX;
                Map<String, Object> map = webRequest.getJsonMapFromUrl(url);
            }
        }

        return securityMetrics;
    }

    private SecurityMetrics setCurrency(String symbol, SecurityMetrics securityMetrics,
            Map<String, String> latestAnnualReport) {
        var returnvalue = securityMetrics;
        if (returnvalue.getCurrencyCode() == null || returnvalue.getCurrencyCode().isEmpty()) {
            returnvalue.setCurrencyCode(latestAnnualReport.get("reportedCurrency"));
        } else if (!returnvalue.getCurrencyCode().equals(latestAnnualReport.get("reportedCurrency"))) {
            auditService.saveMessage("Currency mismatch in report for " + symbol, Severity.ERROR, AUDIT_MSG_TYPE);
        }
        return returnvalue;
    }



    private Double extractDoubleValue(String property, Map<String, Object> report) {
        var value = report.get(property);
        return parseDouble(value);
    }

    private Double parseDouble(Object value) {
        try {
            if (value != null) {
                return Double.parseDouble(value.toString());
            }
        } catch (NumberFormatException e) {
            return 0.0;
        }
        return 0.0;
    }

}
