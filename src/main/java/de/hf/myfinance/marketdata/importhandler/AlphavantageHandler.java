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
import java.util.Optional;
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
    public final static String FX_URLPREFIX = URLPREFIX + FX_FUNCTION + "&from_symbol=";
    public final static String FX_URLPOSTFIX = "&to_symbol=EUR&apikey=" + ALPHAVANTAGE_API_KEY;

    public final static String SECURITYMETRICS_OVERVIEW_FUNCTION = "OVERVIEW";
    public final static String SECURITYMETRICS_OVERVIEW_URLPREFIX = URLPREFIX + SECURITYMETRICS_OVERVIEW_FUNCTION
            + SYMBOL_PREFIX;
    public final static String SECURITYMETRICS_INCOMEVIEW_FUNCTION = "INCOME_STATEMENT";
    public final static String SECURITYMETRICS_INCOMEVIEW_URLPREFIX = URLPREFIX + SECURITYMETRICS_INCOMEVIEW_FUNCTION
            + SYMBOL_PREFIX;
    public final static String SECURITYMETRICS_BALANCEVIEW_FUNCTION = "BALANCE_SHEET";
    public final static String SECURITYMETRICS_BALANCEVIEW_URLPREFIX = URLPREFIX + SECURITYMETRICS_BALANCEVIEW_FUNCTION
            + SYMBOL_PREFIX;
    public final static String SECURITYMETRICS_CASHFLOWVIEW_FUNCTION = "CASH_FLOW";
    public final static String SECURITYMETRICS_CASHFLOWVIEW_URLPREFIX = URLPREFIX
            + SECURITYMETRICS_CASHFLOWVIEW_FUNCTION
            + SYMBOL_PREFIX;
    public final static String SECURITYMETRICS_URLPOSTFIX = "&apikey=" + ALPHAVANTAGE_API_KEY;

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
                    var prices = getTimeSeries("Weekly Time Series", url);
                    add2Pricemap(values, currency, prices);
                });
            }
        } else if (securityType.equals(InstrumentType.CURRENCY)) {
            String currencyCode = security.getAdditionalProperties().get(AdditionalProperties.CURRENCYCODE);
            String url = FX_URLPREFIX + currencyCode + FX_URLPOSTFIX;
            var prices = getTimeSeries("Time Series FX (Daily)", url);
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
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No symbol found for security: " + security.getBusinesskey()));
                securityMetrics = getMockValues(securityMetrics); // For testing purposes, remove in production
                // importOverview(securityMetrics, symbol);
                //securityMetrics = importIncomeView(securityMetrics, symbol);
            }
        }

        return securityMetrics;
    }

    private SecurityMetrics getMockValues(SecurityMetrics securityMetrics) {
        var returnvalue = securityMetrics;
        returnvalue.setCurrencyCode("USD");
        returnvalue.setFiscalEndDate(LocalDate.now());
        returnvalue.setRevenue(10000000.0);
        returnvalue.setEps(5.0);
        returnvalue.setOperatingCashflow(13445000000.0);
        returnvalue.setCapitalExpenditures(1685000000.0);
        return returnvalue;
    }

    private SecurityMetrics importCashFlowView(SecurityMetrics securityMetrics, String symbol) {
        var returnvalue = securityMetrics;
        String url = SECURITYMETRICS_CASHFLOWVIEW_URLPREFIX + symbol + SECURITYMETRICS_URLPOSTFIX;
        Map<String, Object> map = webRequest.getJsonMapFromUrl(url);

        if (map == null || map.isEmpty()) {
            return returnvalue;
        }

        List<Map<String, String>> annualReports = (List<Map<String, String>>) map.get("annualReports");
        List<Map<String, String>> quarterlyReports = (List<Map<String, String>>) map.get("quarterlyReports");

         if (annualReports == null || annualReports.isEmpty()) {
            return returnvalue;
        }

        LocalDate newestAnnualDate = getNewestAnnualDate(annualReports);
        LocalDate newestQuarterlyDate = getNewestQuarterlyDate(quarterlyReports);

        if (newestQuarterlyDate.isAfter(newestAnnualDate)) {

            var latestQuarterlyReport = quarterlyReports.stream()
                    .filter(report -> LocalDate.parse(report.get("fiscalDateEnding")).equals(newestAnnualDate))
                    .findFirst().get();
            
            returnvalue = setCurrency(symbol, returnvalue, latestQuarterlyReport);

            List<Map<String, String>> latest4Reports = getLatest4Reports(quarterlyReports);
            returnvalue.setFiscalEndDate(newestQuarterlyDate);
            
            returnvalue.setTotalAssets(extractValueFromQuarterlyReport(latest4Reports,"totalAssets"));
            returnvalue.setTotalLiabilities(extractValueFromQuarterlyReport(latest4Reports,"totalLiabilities"));
        } else {
            Optional<Map<String, String>> latestAnnualReport = annualReports.stream()
                    .filter(report -> LocalDate.parse(report.get("fiscalDateEnding")).equals(newestAnnualDate))
                    .findFirst();

            if (latestAnnualReport.isPresent()) {
                Map<String, String> report = latestAnnualReport.get();
                returnvalue = setCurrency(symbol, returnvalue, report);
                returnvalue.setFiscalEndDate(newestAnnualDate);
                returnvalue.setTotalAssets(extractDoubleValueFromAnnualReport(report, "totalAssets"));
                returnvalue.setTotalLiabilities(extractDoubleValueFromAnnualReport(report, "totalLiabilities")); // corrected from setRevenue
            } 
        }

        return returnvalue;
    }
    private SecurityMetrics importBalanceView(SecurityMetrics securityMetrics, String symbol) {
        var returnvalue = securityMetrics;
        String url = SECURITYMETRICS_BALANCEVIEW_URLPREFIX + symbol + SECURITYMETRICS_URLPOSTFIX;
        Map<String, Object> map = webRequest.getJsonMapFromUrl(url);

        if (map == null || map.isEmpty()) {
            return returnvalue;
        }

        List<Map<String, String>> annualReports = (List<Map<String, String>>) map.get("annualReports");
        List<Map<String, String>> quarterlyReports = (List<Map<String, String>>) map.get("quarterlyReports");

         if (annualReports == null || annualReports.isEmpty()) {
            return returnvalue;
        }

        LocalDate newestAnnualDate = getNewestAnnualDate(annualReports);
        LocalDate newestQuarterlyDate = getNewestQuarterlyDate(quarterlyReports);

        if (newestQuarterlyDate.isAfter(newestAnnualDate)) {

            var latestQuarterlyReport = quarterlyReports.stream()
                    .filter(report -> LocalDate.parse(report.get("fiscalDateEnding")).equals(newestAnnualDate))
                    .findFirst().get();
            
            returnvalue = setCurrency(symbol, returnvalue, latestQuarterlyReport);

            List<Map<String, String>> latest4Reports = getLatest4Reports(quarterlyReports);
            returnvalue.setFiscalEndDate(newestQuarterlyDate);
            
            returnvalue.setOperatingCashflow(extractValueFromQuarterlyReport(latest4Reports,"operatingCashflow"));
            returnvalue.setCapitalExpenditures(extractValueFromQuarterlyReport(latest4Reports,"capitalExpenditures"));
        } else {
            Optional<Map<String, String>> latestAnnualReport = annualReports.stream()
                    .filter(report -> LocalDate.parse(report.get("fiscalDateEnding")).equals(newestAnnualDate))
                    .findFirst();

            if (latestAnnualReport.isPresent()) {
                Map<String, String> report = latestAnnualReport.get();
                returnvalue = setCurrency(symbol, returnvalue, report);
                returnvalue.setFiscalEndDate(newestAnnualDate);
                returnvalue.setOperatingCashflow(extractDoubleValueFromAnnualReport(report, "operatingCashflow"));
                returnvalue.setCapitalExpenditures(extractDoubleValueFromAnnualReport(report, "capitalExpenditures")); // corrected from setRevenue
            } 
        }

        return returnvalue;
    }

    private SecurityMetrics importIncomeView(SecurityMetrics securityMetrics, String symbol) {
        var returnvalue = securityMetrics;
        String url = SECURITYMETRICS_INCOMEVIEW_URLPREFIX + symbol + SECURITYMETRICS_URLPOSTFIX;
        Map<String, Object> map = webRequest.getJsonMapFromUrl(url);

        if (map == null || map.isEmpty()) {
            return returnvalue;
        }

        List<Map<String, String>> annualReports = (List<Map<String, String>>) map.get("annualReports");
        List<Map<String, String>> quarterlyReports = (List<Map<String, String>>) map.get("quarterlyReports");

        if (annualReports == null || annualReports.isEmpty()) {
            return returnvalue;
        }

        LocalDate newestAnnualDate = getNewestAnnualDate(annualReports);
        LocalDate newestQuarterlyDate = getNewestQuarterlyDate(quarterlyReports);

        if (newestQuarterlyDate.isAfter(newestAnnualDate)) {

            var latestQuarterlyReport = quarterlyReports.stream()
                    .filter(report -> LocalDate.parse(report.get("fiscalDateEnding")).equals(newestAnnualDate))
                    .findFirst().get();
            
            returnvalue = setCurrency(symbol, returnvalue, latestQuarterlyReport);

            List<Map<String, String>> latest4Reports = getLatest4Reports(quarterlyReports);
            returnvalue.setFiscalEndDate(newestQuarterlyDate);
            
            returnvalue.setRevenue(extractValueFromQuarterlyReport(latest4Reports,"totalRevenue"));
            returnvalue.setNetIncome(extractValueFromQuarterlyReport(latest4Reports,"netIncome"));
        } else {
            Optional<Map<String, String>> latestAnnualReport = annualReports.stream()
                    .filter(report -> LocalDate.parse(report.get("fiscalDateEnding")).equals(newestAnnualDate))
                    .findFirst();

            if (latestAnnualReport.isPresent()) {
                Map<String, String> report = latestAnnualReport.get();
                returnvalue = setCurrency(symbol, returnvalue, report);
                returnvalue.setFiscalEndDate(newestAnnualDate);
                returnvalue.setRevenue(extractDoubleValueFromAnnualReport(report, "totalRevenue"));
                returnvalue.setNetIncome(extractDoubleValueFromAnnualReport(report, "netIncome")); // corrected from setRevenue
            } 
        }

        Map<LocalDate, Double> historicalRevenue = new HashMap<>();
        Map<LocalDate, Double> historicalNetIncome = new HashMap<>();
        annualReports.forEach(report -> {
            var date = LocalDate.parse(report.get("fiscalDateEnding"));
            historicalRevenue.put(date, extractDoubleValueFromAnnualReport(report, "totalRevenue"));
            historicalNetIncome.put(date, extractDoubleValueFromAnnualReport(report, "netIncome"));
        });
        returnvalue.setHistoricalRevenue(historicalRevenue);
        returnvalue.setHistoricalNetIncome(historicalNetIncome);
        return returnvalue;
    }

    private SecurityMetrics setCurrency(String symbol, SecurityMetrics securityMetrics,
            Map<String, String> latestAnnualReport) {
        var returnvalue = securityMetrics;
        if(securityMetrics.getCurrencyCode() == null || securityMetrics.getCurrencyCode().isEmpty()) {
            returnvalue.setCurrencyCode(latestAnnualReport.get("currency"));
        } else  if (!returnvalue.getCurrencyCode().equals(latestAnnualReport.get("currency"))) {
            auditService.saveMessage("Currency mismatch in quarterly report for " + symbol, Severity.ERROR, AUDIT_MSG_TYPE);
        }
        return returnvalue;
    }

    private double extractDoubleValueFromAnnualReport(Map<String, String> report, String propertyKey) {
        return Double.parseDouble(report.get("totalRevenue"));
    }

    private double extractValueFromQuarterlyReport(List<Map<String, String>> latest4Reports, String propertyKey) {
        return latest4Reports.stream()
                .mapToDouble(report -> Double.parseDouble(report.get(propertyKey)))
                .sum();
    }

    private List<Map<String, String>> getLatest4Reports(List<Map<String, String>> quarterlyReports) {
        List<Map<String, String>> latest4Reports = quarterlyReports.stream()
                .sorted(Comparator
                        .comparing((Map<String, String> report) -> LocalDate.parse(report.get("fiscalDateEnding")))
                        .reversed())
                .limit(4)
                .toList();
        return latest4Reports;
    }

    private LocalDate getNewestQuarterlyDate(List<Map<String, String>> quarterlyReports) {
        LocalDate newestQuarterlyDate = LocalDate.MIN;
        if (quarterlyReports != null && !quarterlyReports.isEmpty()) {
            newestQuarterlyDate = quarterlyReports.stream()
                    .map(report -> LocalDate.parse(report.get("fiscalDateEnding")))
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.MIN);
        }
        return newestQuarterlyDate;
    }

    private LocalDate getNewestAnnualDate(List<Map<String, String>> annualReports) {
        LocalDate newestAnnualDate = annualReports.stream()
                .map(report -> LocalDate.parse(report.get("fiscalDateEnding")))
                .max(LocalDate::compareTo)
                .orElse(LocalDate.MIN);
        return newestAnnualDate;
    }

    private SecurityMetrics importOverview(SecurityMetrics securityMetrics, String symbol) {
        var returnvalue = securityMetrics;
        String url = SECURITYMETRICS_OVERVIEW_URLPREFIX + symbol + SECURITYMETRICS_URLPOSTFIX;
        Map<String, Object> map = webRequest.getJsonMapFromUrl(url);
        if (map != null && !map.isEmpty()) {
            returnvalue.setCurrencyCode(map.get("Currency").toString());
            returnvalue.setSector(map.get("Sector").toString());
            returnvalue.setDividendPerShare(Double.valueOf(map.get("DividendPerShare").toString()));
            returnvalue.setEps(Double.valueOf(map.get("EPS").toString()));
            returnvalue.setSharesOutstanding(Double.valueOf(map.get("SharesOutstanding").toString()));
            returnvalue.setRevenue(Double.valueOf(map.get("RevenueTTM").toString()));
            returnvalue.setBeta(Double.valueOf(map.get("Beta").toString()));
        }
        return returnvalue;
    }

}
