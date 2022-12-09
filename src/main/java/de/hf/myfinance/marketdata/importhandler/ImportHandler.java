package de.hf.myfinance.marketdata.importhandler;


import de.hf.myfinance.restmodel.EndOfDayPrice;
import de.hf.myfinance.restmodel.Instrument;

import java.time.LocalDate;
import java.util.Map;

public interface ImportHandler {
    Map<LocalDate, EndOfDayPrice> importPrices(Instrument security);
}
