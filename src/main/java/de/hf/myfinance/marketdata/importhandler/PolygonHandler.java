package de.hf.myfinance.marketdata.importhandler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

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

@Component
public class PolygonHandler implements ImportHandler {

    private final DataReaderImpl dataReaderImpl;

    WebRequest webRequest;
    AuditService auditService;

    // https://api.polygon.io/v2/aggs/ticker/SAP/prev?apiKey=bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p
    // https://api.polygon.io/v2/aggs/ticker/C:USDEUR/prev?apiKey=bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p
    // https://api.polygon.io/vX/reference/financials?ticker=MSFT&timeframe=ttm&order=asc&limit=10&sort=filing_date&apiKey=bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p

    public final static String API_KEY = "bE0SPbXzJaCmVasf67Y0gdb4fOExSQ4p"; // Replace with your actual API key
    public final static String URLPREFIX = "https://api.polygon.io/";
    public final static String PRICE_PREFIX = "v2/aggs/ticker/";

    public final static String URLPOSTFIX = "apiKey=" + API_KEY;

    public final static String PRICE_URLPOSTFIX = "/prev?" + URLPOSTFIX;
    public final static String PRICE_URLPREFIX = URLPREFIX + PRICE_PREFIX;
    public final static String FX_URLPREFIX = PRICE_URLPREFIX + "C:";
    public final static String FX_URLPOSTFIX = "EUR" + PRICE_URLPOSTFIX;
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
            String url = FX_URLPREFIX + currencyCode + FX_URLPOSTFIX;
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

        try{
            Map<String, Object> map = webRequest.getJsonMapFromUrl(url);
            Map<String, Object> timeSeries = (Map<String, Object>) ((List<Object>) map.get("results")).get(0);
            if (timeSeries == null) {
                auditService.saveMessage("invalid request or no data for url " + url, Severity.ERROR, AUDIT_MSG_TYPE);
                if (map != null && map.containsKey("Error Message")) {
                    auditService.saveMessage("Polygon report an error: " + map.get("Error Message"), Severity.ERROR,
                            AUDIT_MSG_TYPE);
                }
            } else{
                LocalDate date = LocalDate.now().minusDays(1);
                Double value = Double.parseDouble(timeSeries.get("c").toString());
                prices.put(date, value);
            }
        }catch (Exception e) {
            auditService.saveMessage("invalid request or no data for url " + url, Severity.ERROR, AUDIT_MSG_TYPE);
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
                .filter(i -> (i.getInstrumentType().equals(InstrumentType.CURRENCY) 
                            && i.getAdditionalProperties() != null 
                            && i.getAdditionalProperties().get(AdditionalProperties.CURRENCYCODE) != null
                            && !i.getAdditionalProperties().get(AdditionalProperties.CURRENCYCODE).equals("EUR")
                        )
                        || (
                            i.getAdditionalMaps() != null &&
                            i.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS) != null &&
                            // keep only if NO key contains a dot
                            i.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS).keySet().stream().noneMatch(k -> k.contains("."))
                        )
                )
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'importSecurityMetrics'");
    }

}
