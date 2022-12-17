package de.hf.myfinance.marketdata.persistence.repositories;

import de.hf.myfinance.marketdata.persistence.entities.InstrumentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface InstrumentRepository extends ReactiveCrudRepository<InstrumentEntity, String> {
    Mono<InstrumentEntity> findByBusinesskey(String businesskey);
    Flux<InstrumentEntity> findByActive(Boolean active);
    Flux<InstrumentEntity> findByBusinesskeyIn(Iterable<String> businesskeyIterable);
    Mono<Long> deleteByBusinesskey(String businesskey);
}
