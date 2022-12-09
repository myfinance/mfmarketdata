package de.hf.myfinance.marketdata.service;

import de.hf.myfinance.marketdata.events.out.EventHandler;
import de.hf.myfinance.marketdata.importhandler.ImportHandler;
import de.hf.myfinance.marketdata.persistence.DataReader;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MarketDataService {
    private final DataReader dataReader;
    private final ImportHandler importhandler;
    private final EventHandler eventHandler;

    @Autowired
    public MarketDataService(DataReader dataReader, ImportHandler importhandler, EventHandler eventHandler) {
        this.dataReader = dataReader;
        this.importhandler = importhandler;
        this.eventHandler = eventHandler;
    }

    public Flux<Void> importData(){
        return dataReader.findAllInstruments()
                .flatMap(i->importPrices4Instrument(i));
    }

    private Mono<Void> importPrices4Instrument(Instrument instrument) {
        return dataReader.findPrices4Instrument(instrument.getBusinesskey())
                .switchIfEmpty(Mono.just(new EndOfDayPrices(instrument.getBusinesskey())))
                .flatMap(p->{
                    var oldPrices = p.getPrices();
                    oldPrices.putAll(importhandler.importPrices(instrument));
                    p.setPrices(oldPrices);
                    return Mono.just(p);
                })
                .flatMap(p->{
                    eventHandler.sendPricesUpdatedEvent(p);
                    return Mono.just("").then();
                });
    }
}
