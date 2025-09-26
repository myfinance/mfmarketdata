package de.hf.myfinance.marketdata.persistence.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import de.hf.myfinance.restmodel.RiskProfile;
import de.hf.myfinance.restmodel.SecurityLifecyclePhase;

@Document(collection = "SecurityMetrics")
public class SecurityMetricsEntity {
    @Id
    private String securityMetricsid;
    @Version
    private Integer version;

    @Indexed(unique = true)
    String businesskey;
    String description;
    // the currency code of the security, e.g. EUR, USD, GBP just for display purposes
    String currencyCode;
    //the businesskey of the currency to identify the value curve of the currency and calculate the price in Euro
    String currencyKey;
    LocalDate fiscalEndDate;
    SecurityLifecyclePhase securityLifecyclePhase;
    RiskProfile riskProfile;
    String sector;
    LocalDateTime lastUpdateTs;


    // all values are yearly TTM values
    //mandatory
    Double price;
    Double priceInEuro;
    Double sharesOutstanding;
    Double revenue;
    Double capitalExpenditures;
    Double operatingCashflow;
    Double netIncome;

    //optional
    Double totalAssets;
    Double totalLiabilities;
    Double dilutedEPS5Y;
    Double dividendPerShare;
    Double forwardFreeCashflow5YCAGR;
    Double forwardPriceToSales;
    Double beta;
    Double tam;
    Double forwardPE;
    Double minForwardFCF10YCAGR;
    Double avgForwardFCF10YCAGR;
    Double maxForwardFCF10YCAGR;
    


    //calculated
    Double freeCashflow;
    Double expectedFreeCashflow;
    Double avgFreeCashflow5Y;
    Double avgFreeCashflowGrowth5Y;
    Double pe;
    Double roa;
    Double debtToAssets;
    Double dividendYield;
    Double dividendPayoutRatio;
    Double intrinsicValue;
    Double intrinsicValueMargin;
    Double intrinsicValueEVMargin;
    Double lynchScore;
    Double revenueGrowthRate;
    Double eps;

    //historical map<fiscalaenddate, value>. fiscalaenddate is a Date, the values are TTM(trailing twelve month) values
    Map<Integer, Double> historicalRevenue;
    Map<Integer, Double> historicalNetIncome;
    Map<Integer, Double> historicalFreeCashflow;
    Map<Integer, Double> expectedFreeCashflowPerYear;

    public String getBusinesskey() {
        return this.businesskey;
    }

    public void setBusinesskey(String businesskey) {
        this.businesskey = businesskey;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDate getFiscalEndDate() {
        return this.fiscalEndDate;
    }

    public void setFiscalEndDate(LocalDate fiscalEndDate) {
        this.fiscalEndDate = fiscalEndDate;
    }

    public SecurityLifecyclePhase getSecurityLifecyclePhase() {
        return this.securityLifecyclePhase;
    }

    public void setSecurityLifecyclePhase(SecurityLifecyclePhase securityLifecyclePhase) {
        this.securityLifecyclePhase = securityLifecyclePhase;
    }

    public RiskProfile getRiskProfile() {
        return this.riskProfile;
    }

    public void setRiskProfile(RiskProfile riskProfile) {
        this.riskProfile = riskProfile;
    }

    public String getSector() {
        return this.sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public Double getPrice() {
        return this.price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getSharesOutstanding() {
        return this.sharesOutstanding;
    }

    public void setSharesOutstanding(Double sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    public Double getRevenue() {
        return this.revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue;
    }

    public Double getEps() {
        return this.eps;
    }

    public void setEps(Double eps) {
        this.eps = eps;
    }

    public Double getFreeCashflow() {
        return this.freeCashflow;
    }

    public void setFreeCashflow(Double freeCashflow) {
        this.freeCashflow = freeCashflow;
    }

    public Double getTotalAssets() {
        return this.totalAssets;
    }

    public void setTotalAssets(Double totalAssets) {
        this.totalAssets = totalAssets;
    }

    public Double getTotalLiabilities() {
        return this.totalLiabilities;
    }

    public void setTotalLiabilities(Double totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public Double getDilutedEPS5Y() {
        return this.dilutedEPS5Y;
    }

    public void setDilutedEPS5Y(Double dilutedEPS5Y) {
        this.dilutedEPS5Y = dilutedEPS5Y;
    }

    public Double getDividendPerShare() {
        return this.dividendPerShare;
    }

    public void setDividendPerShare(Double dividendPerShare) {
        this.dividendPerShare = dividendPerShare;
    }

    public Double getForwardFreeCashflow5YCAGR() {
        return this.forwardFreeCashflow5YCAGR;
    }

    public void setForwardFreeCashflow5YCAGR(Double forwardFreeCashflow5YCAGR) {
        this.forwardFreeCashflow5YCAGR = forwardFreeCashflow5YCAGR;
    }

    public Double getForwardPriceToSales() {
        return this.forwardPriceToSales;
    }

    public void setForwardPriceToSales(Double forwardPriceToSales) {
        this.forwardPriceToSales = forwardPriceToSales;
    }

    public Double getBeta() {
        return this.beta;
    }

    public void setBeta(Double beta) {
        this.beta = beta;
    }

    public Double getTam() {
        return this.tam;
    }

    public void setTam(Double tam) {
        this.tam = tam;
    }

    public Double getForwardPE() {
        return this.forwardPE;
    }

    public void setForwardPE(Double forwardPE) {
        this.forwardPE = forwardPE;
    }

    public Double getMinForwardFCF10YCAGR() {
        return this.minForwardFCF10YCAGR;
    }

    public void setMinForwardFCF10YCAGR(Double minForwardFCF10YCAGR) {
        this.minForwardFCF10YCAGR = minForwardFCF10YCAGR;
    }

    public Double getAvgForwardFCF10YCAGR() {
        return this.avgForwardFCF10YCAGR;
    }

    public void setAvgForwardFCF10YCAGR(Double avgForwardFCF10YCAGR) {
        this.avgForwardFCF10YCAGR = avgForwardFCF10YCAGR;
    }

    public Double getMaxForwardFCF10YCAGR() {
        return this.maxForwardFCF10YCAGR;
    }

    public void setMaxForwardFCF10YCAGR(Double maxForwardFCF10YCAGR) {
        this.maxForwardFCF10YCAGR = maxForwardFCF10YCAGR;
    }

    public Double getNetIncome() {
        return this.netIncome;
    }

    public void setNetIncome(Double netIncome) {
        this.netIncome = netIncome;
    }

    public Double getPe() {
        return this.pe;
    }

    public void setPe(Double pe) {
        this.pe = pe;
    }

    public Double getRoa() {
        return this.roa;
    }

    public void setRoa(Double roa) {
        this.roa = roa;
    }

    public Double getDebtToAssets() {
        return this.debtToAssets;
    }

    public void setDebtToAssets(Double debtToAssets) {
        this.debtToAssets = debtToAssets;
    }

    public Double getDividendYield() {
        return this.dividendYield;
    }

    public void setDividendYield(Double dividendYield) {
        this.dividendYield = dividendYield;
    }

    public Double getDividendPayoutRatio() {
        return this.dividendPayoutRatio;
    }

    public void setDividendPayoutRatio(Double dividendPayoutRatio) {
        this.dividendPayoutRatio = dividendPayoutRatio;
    }

    public Double getIntrinsicValue() {
        return this.intrinsicValue;
    }

    public void setIntrinsicValue(Double intrinsicValue) {
        this.intrinsicValue = intrinsicValue;
    }

    public Double getIntrinsicValueMargin() {
        return this.intrinsicValueMargin;
    }

    public void setIntrinsicValueMargin(Double intrinsicValueMargin) {
        this.intrinsicValueMargin = intrinsicValueMargin;
    }

    public Double getLynchScore() {
        return this.lynchScore;
    }

    public void setLynchScore(Double lynchScore) {
        this.lynchScore = lynchScore;
    }

    public Double getRevenueGrowthRate() {
        return this.revenueGrowthRate;
    }

    public void setRevenueGrowthRate(Double revenueGrowthRate) {
        this.revenueGrowthRate = revenueGrowthRate;
    }    

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPriceInEuro() {
        return this.priceInEuro;
    }

    public void setPriceInEuro(Double priceInEuro) {
        this.priceInEuro = priceInEuro;
    }
    

    public Double getCapitalExpenditures() {
        return this.capitalExpenditures;
    }

    public void setCapitalExpenditures(Double capitalExpenditures) {
        this.capitalExpenditures = capitalExpenditures;
    }

    public Double getOperatingCashflow() {
        return this.operatingCashflow;
    }

    public void setOperatingCashflow(Double operatingCashflow) {
        this.operatingCashflow = operatingCashflow;
    }


    public String getCurrencyKey() {
        return this.currencyKey;
    }

    public void setCurrencyKey(String currencyKey) {
        this.currencyKey = currencyKey;
    }

    public LocalDateTime getLastUpdateTs() {
        return this.lastUpdateTs;
    }

    public void setLastUpdateTs(LocalDateTime lastUpdateTs) {
        this.lastUpdateTs = lastUpdateTs;
    }


    public String getSecurityMetricsid() {
        return this.securityMetricsid;
    }

    public void setSecurityMetricsid(String securityMetricsid) {
        this.securityMetricsid = securityMetricsid;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Double getExpectedFreeCashflow() {
        return this.expectedFreeCashflow;
    }

    public void setExpectedFreeCashflow(Double expectedFreeCashflow) {
        this.expectedFreeCashflow = expectedFreeCashflow;
    }

    public Double getAvgFreeCashflow5Y() {
        return this.avgFreeCashflow5Y;
    }

    public void setAvgFreeCashflow5Y(Double avgFreeCashflow5Y) {
        this.avgFreeCashflow5Y = avgFreeCashflow5Y;
    }

    public Double getAvgFreeCashflowGrowth5Y() {
        return this.avgFreeCashflowGrowth5Y;
    }

    public void setAvgFreeCashflowGrowth5Y(Double avgFreeCashflowGrowth5Y) {
        this.avgFreeCashflowGrowth5Y = avgFreeCashflowGrowth5Y;
    }

    public Double getIntrinsicValueEVMargin() {
        return this.intrinsicValueEVMargin;
    }

    public void setIntrinsicValueEVMargin(Double intrinsicValueEVMargin) {
        this.intrinsicValueEVMargin = intrinsicValueEVMargin;
    }

    public Map<Integer,Double> getHistoricalRevenue() {
        return this.historicalRevenue;
    }

    public void setHistoricalRevenue(Map<Integer,Double> historicalRevenue) {
        this.historicalRevenue = historicalRevenue;
    }

    public Map<Integer,Double> getHistoricalNetIncome() {
        return this.historicalNetIncome;
    }

    public void setHistoricalNetIncome(Map<Integer,Double> historicalNetIncome) {
        this.historicalNetIncome = historicalNetIncome;
    }

    public Map<Integer,Double> getHistoricalFreeCashflow() {
        return this.historicalFreeCashflow;
    }

    public void setHistoricalFreeCashflow(Map<Integer,Double> historicalFreeCashflow) {
        this.historicalFreeCashflow = historicalFreeCashflow;
    }

    public Map<Integer,Double> getExpectedFreeCashflowPerYear() {
        return this.expectedFreeCashflowPerYear;
    }

    public void setExpectedFreeCashflowPerYear(Map<Integer,Double> expectedFreeCashflowPerYear) {
        this.expectedFreeCashflowPerYear = expectedFreeCashflowPerYear;
    }

}
