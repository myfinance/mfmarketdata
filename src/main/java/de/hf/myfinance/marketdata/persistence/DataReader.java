package de.hf.myfinance.marketdata.persistence;


import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataReader {
    Flux<Instrument> findInstrumentByBusinesskeyIn(Iterable<String> businesskeyIterable);
    Flux<Instrument> findAllInstruments();
    Flux<Instrument> findActiveInstruments();
    Mono<EndOfDayPrices> findPrices4Instrument(String instrumentBusinesskey);
    Mono<Instrument> findByBusinesskey(String businesskey); 
    Flux<KeyTsProjection> getKeyToTsMap();
}
