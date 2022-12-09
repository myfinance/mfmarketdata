package de.hf.myfinance.marketdata.persistence;


import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface DataReader {
    Flux<Instrument> findInstrumentByBusinesskeyIn(Iterable<String> businesskeyIterable);
    Flux<Instrument> findAllInstruments();
    Mono<EndOfDayPrices> findPrices4Instrument(String instrumentBusinesskey);
}
