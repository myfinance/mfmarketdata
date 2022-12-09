package de.hf.myfinance.marketdata.persistence.entities;

import de.hf.myfinance.restmodel.AdditionalMaps;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.InstrumentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;


@Document(collection = "instruments")
public class InstrumentEntity implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String instrumentid;
    @Version
    private Integer version;
    private InstrumentType instrumentType;
    private boolean active;
    @Indexed(unique = true)
    private String businesskey;
    private Map<AdditionalMaps, Map<String, String>> additionalMaps = new HashMap<>();
    private Map<AdditionalProperties, String> additionalProperties = new HashMap<>();

    public InstrumentEntity() {
    }

    public InstrumentEntity(String businesskey, InstrumentType instrumentType, boolean active) {
        setInstrumentType(instrumentType);
        this.businesskey = businesskey;
        this.active = active;
    }

    public String getInstrumentid() {
        return this.instrumentid;
    }
    public void setInstrumentid(String instrumentid) {
        this.instrumentid = instrumentid;
    }

    public InstrumentType getInstrumentType(){
        return instrumentType;
    }
    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public String getBusinesskey() {
        return this.businesskey;
    }
    public void setBusinesskey(String businesskey) {
        this.businesskey = businesskey;
    }


    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }

    public Map<AdditionalMaps, Map<String, String>> getAdditionalMaps() {
        return additionalMaps;
    }
    public void setAdditionalMaps(Map<AdditionalMaps, Map<String, String>> additionalMaps) {
        this.additionalMaps = additionalMaps;
    }

    public Map<AdditionalProperties, String> getAdditionalProperties() {
        return additionalProperties;
    }
    public void setAdditionalProperties(Map<AdditionalProperties, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}