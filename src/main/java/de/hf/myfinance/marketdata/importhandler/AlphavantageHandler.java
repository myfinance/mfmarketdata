package de.hf.myfinance.marketdata.importhandler;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.marketdata.webtools.WebRequest;
import de.hf.myfinance.restmodel.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AlphavantageHandler implements ImportHandler {

    WebRequest webRequest;
    AuditService auditService;

    public final static String EQ_URLPREFIX="https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol=";
    public final static String EQ_URLPOSTFIX="&apikey=Q6RLS6PGB55105EP";
    public final static String FX_URLPREFIX="https://www.alphavantage.co/query?function=FX_DAILY&from_symbol=";
    public final static String FX_URLPOSTFIX="&to_symbol=EUR&apikey=Q6RLS6PGB55105EP";

    protected static final String AUDIT_MSG_TYPE="AlphavantageHandler_Event";


    public AlphavantageHandler(WebRequest webRequest, AuditService auditService){
        this.webRequest=webRequest;
        this.auditService=auditService;
    }

    public Map<LocalDate, EndOfDayPrice> importPrices(Instrument security){
        final Map<LocalDate, EndOfDayPrice> values = new HashMap<>();
        InstrumentType securityType = security.getInstrumentType();
        if(securityType.equals(InstrumentType.EQUITY)) {
            var symbols = security.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS);
            symbols.keySet().forEach(s->{
                String url = EQ_URLPREFIX+s+EQ_URLPOSTFIX;
                var currency = symbols.get(s);
                var prices = getTimeSeries(security, "Weekly Time Series", url);
                add2Pricemap(values, currency, prices);
            });
        }else if(securityType.equals(InstrumentType.CURRENCY)) {
            String currencyCode = security.getAdditionalProperties().get(AdditionalProperties.CURRENCYCODE);
            String url = FX_URLPREFIX+currencyCode+FX_URLPOSTFIX;
            var prices = getTimeSeries(security, "Time Series FX (Daily)", url);
            add2Pricemap(values, Instrument.DEFAULTCURRENCY, prices);
        }

        return values;
    }

    private Map<LocalDate, EndOfDayPrice> add2Pricemap(Map<LocalDate, EndOfDayPrice> values, String currency, Map<LocalDate, Double> prices) {
        prices.entrySet().forEach(entry->{
            var endOfDayPrice = new EndOfDayPrice(entry.getValue(), currency);
            values.put(entry.getKey(), endOfDayPrice);
        });
        return values;
    }

    private Map<LocalDate, Double> getTimeSeries(Instrument security, String timeSeriesName, String url) {
        Map<LocalDate, Double> prices = new HashMap<>();
        Map<String, Object> map = webRequest.getJsonMapFromUrl(url);
        Map<String, Object> timeSeries = (Map<String, Object>)map.get(timeSeriesName);
        if(timeSeries==null){
            auditService.saveMessage("invalid request or no data for url " + url, Severity.ERROR, AUDIT_MSG_TYPE);
            if(map !=null && map.containsKey("Error Message")){
                auditService.saveMessage("Alphavantage report an error: " + map.get("Error Message"), Severity.ERROR, AUDIT_MSG_TYPE);
            }
        } else {
            for (String dateString: timeSeries.keySet() ) {
                try {
                    LocalDate date = LocalDate.parse(dateString);
                    String valueString = ((Map<String, String>) timeSeries.get(dateString)).get("4. close");
                    Double value = Double.parseDouble(valueString);

                    prices.put(date, value);

                } catch (Exception e) {
                    auditService.saveMessage("can not parse value for date" + dateString + " and url " + url, Severity.ERROR, AUDIT_MSG_TYPE);
                }
            }
        }
        return prices;
    }



}
