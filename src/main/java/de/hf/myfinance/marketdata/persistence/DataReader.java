package de.hf.myfinance.marketdata.persistence;

import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.SecurityMetrics;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataReader {
    Flux<Instrument> findInstrumentByBusinesskeyIn(Iterable<String> businesskeyIterable);
    Flux<Instrument> findAllInstruments();
    Flux<Instrument> findActiveInstruments();
    Mono<EndOfDayPrices> findPrices4Instrument(String instrumentBusinesskey);
    Mono<Instrument> findByBusinesskey(String businesskey); 
    Flux<KeyTsProjection> getKeyToTsMap();
    Mono<SecurityMetrics> findSecurityMetrics4Instrument(String instrumentBusinesskey);
    Flux<KeyTsProjection> getSecurityMetricsKeyToTsMap();
    Mono<Instrument> findCurrencyByCurrencyCode(String CurrencyCode);
}
