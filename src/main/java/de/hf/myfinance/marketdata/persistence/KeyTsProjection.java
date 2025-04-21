package de.hf.myfinance.marketdata.persistence;

import java.time.LocalDateTime;

public interface KeyTsProjection {

    String getInstrumentBusinesskey();
    LocalDateTime getLastUpdateTs(); 

}
