package de.hf.myfinance.marketdata.persistence;

import de.hf.myfinance.marketdata.persistence.repositories.EndOfDayPricesRepository;
import de.hf.myfinance.marketdata.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Component
public class DataReaderImpl implements DataReader{
    private final InstrumentRepository instrumentRepository;
    private final EndOfDayPricesRepository endOfDayPricesRepository;
    private final InstrumentMapper instrumentMapper;
    private final EndOfDayPricesMapper endOfDayPricesMapper;

    public DataReaderImpl(InstrumentRepository instrumentRepository, EndOfDayPricesRepository endOfDayPricesRepository, InstrumentMapper instrumentMapper, EndOfDayPricesMapper endOfDayPricesMapper) {
        this.instrumentRepository = instrumentRepository;
        this.endOfDayPricesRepository = endOfDayPricesRepository;
        this.instrumentMapper = instrumentMapper;
        this.endOfDayPricesMapper = endOfDayPricesMapper;
    }

    @Override
    public Flux<Instrument> findInstrumentByBusinesskeyIn(Iterable<String> businesskeyIterable) {
        return instrumentRepository.findByBusinesskeyIn(businesskeyIterable)
                .map(e->
                        instrumentMapper.entityToApi(e)
                );
    }

    @Override
    public Flux<Instrument> findAllInstruments() {
        return instrumentRepository.findAll()
                .map(e->
                        instrumentMapper.entityToApi(e)
                );
    }

    @Override
    public Flux<Instrument> findActiveInstruments() {
        return instrumentRepository.findByActive(true)
                .map(e->
                        instrumentMapper.entityToApi(e)
                );
    }

    @Override
    public Mono<EndOfDayPrices> findPrices4Instrument(String instrumentBusinesskey) {
        return endOfDayPricesRepository.findByInstrumentBusinesskey(instrumentBusinesskey)
                .map(e->
                        endOfDayPricesMapper.entityToApi(e)
                );
    }

    @Override
    public Mono<Instrument> findByBusinesskey(String businesskey) {
        return instrumentRepository.findByBusinesskey(businesskey).map(e-> instrumentMapper.entityToApi(e));
    }

    @Override
    public Flux<KeyTsProjection> getKeyToTsMap() {
        return endOfDayPricesRepository.findAllBy();
    }

}
