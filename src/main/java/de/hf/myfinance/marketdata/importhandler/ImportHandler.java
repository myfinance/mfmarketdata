package de.hf.myfinance.marketdata.importhandler;


import de.hf.myfinance.marketdata.persistence.KeyTsProjection;
import de.hf.myfinance.restmodel.EndOfDayPrice;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.SecurityMetrics;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ImportHandler {
    Map<LocalDate, EndOfDayPrice> importPrices(Instrument security);
    Mono<List<Instrument>> filterInstruments(List<Instrument> instruments, Flux<KeyTsProjection> keyTsFlux);
    SecurityMetrics importSecurityMetrics(Instrument security);
}
