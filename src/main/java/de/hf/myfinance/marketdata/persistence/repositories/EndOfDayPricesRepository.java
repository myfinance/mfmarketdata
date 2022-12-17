package de.hf.myfinance.marketdata.persistence.repositories;

import de.hf.myfinance.marketdata.persistence.entities.EndOfDayPricesEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EndOfDayPricesRepository  extends ReactiveCrudRepository<EndOfDayPricesEntity, String> {
    Mono<EndOfDayPricesEntity> findByInstrumentBusinesskey(String instrumentBusinesskey);
    Mono<Long> deleteByInstrumentBusinesskey(String instrumentBusinesskey);
}
