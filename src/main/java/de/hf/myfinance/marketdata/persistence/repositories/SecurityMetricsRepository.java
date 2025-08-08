package de.hf.myfinance.marketdata.persistence.repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import de.hf.myfinance.marketdata.persistence.KeyTsProjection;
import de.hf.myfinance.marketdata.persistence.entities.SecurityMetricsEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SecurityMetricsRepository extends ReactiveCrudRepository<SecurityMetricsEntity, String> {
    Mono<SecurityMetricsEntity> findByBusinesskey(String businesskey);
    Mono<Long> deleteByBusinesskey(String businesskey);

    Flux<KeyTsProjection> findAllBy();
}
