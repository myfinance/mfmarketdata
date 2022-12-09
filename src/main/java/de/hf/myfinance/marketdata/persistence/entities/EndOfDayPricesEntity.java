package de.hf.myfinance.marketdata.persistence.entities;

import de.hf.myfinance.restmodel.EndOfDayPrice;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Map;

@Document(collection = "prices")
public class EndOfDayPricesEntity {
    @Id
    private String priceid;
    @Version
    private Integer version;

    @Indexed(unique = true)
    private String instrumentBusinesskey;
    private Map<LocalDate, EndOfDayPrice> prices;

    public String getInstrumentBusinesskey() {
        return instrumentBusinesskey;
    }
    public void setInstrumentBusinesskey(String instrumentBusinesskey) {
        this.instrumentBusinesskey = instrumentBusinesskey;
    }

    public Map<LocalDate, EndOfDayPrice> getPrices() {
        return prices;
    }
    public void setPrices(Map<LocalDate, EndOfDayPrice> prices) {
        this.prices = prices;
    }

    public String getPriceid() {
        return priceid;
    }
    public void setPriceid(String priceid) {
        this.priceid = priceid;
    }
}
