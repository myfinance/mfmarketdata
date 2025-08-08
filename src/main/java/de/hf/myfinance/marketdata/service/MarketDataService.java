package de.hf.myfinance.marketdata.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.marketdata.events.out.PriceUpdateEventHandler;
import de.hf.myfinance.marketdata.events.out.SecurityMetricsImportedEventHandler;
import de.hf.myfinance.marketdata.importhandler.ImportHandler;
import de.hf.myfinance.marketdata.persistence.DataReader;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.SecurityMetrics;

import java.security.Security;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MarketDataService {
    private final DataReader dataReader;
    private final ImportHandler importhandler;
    private final PriceUpdateEventHandler priceUpdateEventHandler;
    private final SecurityMetricsImportedEventHandler securityMetricsImportedEventHandler;
    protected final AuditService auditService;

    protected static final String AUDIT_MSG_TYPE="MarketDataService_User_Event";

    public MarketDataService(DataReader dataReader, ImportHandler importhandler, PriceUpdateEventHandler priceUpdateEventHandler, SecurityMetricsImportedEventHandler securityMetricsImportedEventHandler, AuditService auditService) {
        this.dataReader = dataReader;
        this.importhandler = importhandler;
        this.priceUpdateEventHandler = priceUpdateEventHandler;
        this.securityMetricsImportedEventHandler = securityMetricsImportedEventHandler;
        this.auditService = auditService;
    }

    public Mono<Void> savePrices(EndOfDayPrices endOfDayPrices) {
        return dataReader.findPrices4Instrument(endOfDayPrices.getInstrumentBusinesskey())
                .switchIfEmpty(Mono.just(new EndOfDayPrices(endOfDayPrices.getInstrumentBusinesskey())))
                .flatMap(p -> {
                    var oldPrices = p.getPrices();
                    oldPrices.putAll(endOfDayPrices.getPrices());
                    p.setPrices(oldPrices);
                    return Mono.just(p);
                })

                .flatMap(p -> {
                    priceUpdateEventHandler.sendPricesUpdatedEvent(p);
                    return Mono.just("").then();
                });
    }

    public Flux<Void> importData() {
        return dataReader.findActiveInstruments()
            .collectList()
            .flatMap(instrumentList->importhandler.filterInstruments(instrumentList, dataReader.getKeyToTsMap()))
            .flatMapMany(Flux::fromIterable)
            .flatMap(i->importPrices4Instrument(i));
    }

    public Mono<Void> importData4Instrument(String businesskey) {
        return dataReader.findByBusinesskey(businesskey)
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
                    if(p!=null && p.getPrices()!=null && !p.getPrices().isEmpty()){
                        p.setLastUpdateTs(LocalDateTime.now());
                        priceUpdateEventHandler.sendPricesUpdatedEvent(p);
                    }
                    return Mono.just("").then();
                });
    }

    public Mono<EndOfDayPrices> getEndOfDayPrices(String businesskey) {
        return dataReader.findPrices4Instrument(businesskey);
    }

    public Mono<Void> validateAndSavePrices(EndOfDayPrices endOfDayPrices) {
        return loadAndValidateInstrument(endOfDayPrices.getInstrumentBusinesskey())
            .flatMap(i->{
                return this.savePrices(endOfDayPrices);
            });

    }

    private Mono<Instrument> loadAndValidateInstrument(String businesskey) {
        return this.dataReader.findByBusinesskey(businesskey)
                .switchIfEmpty(handleNotExistingInstrument(businesskey));
    }

    private Mono<Instrument> handleNotExistingInstrument(String businesskey) {
        return auditService.handleMonoError("Instrument for businesskey:" + businesskey + " does not exists.",
                    AUDIT_MSG_TYPE, MFMsgKey.UNKNOWN_INSTRUMENT_EXCEPTION).cast(Instrument.class);

    }

    public Flux<SecurityMetrics> importSecurityMetrics() {
        return dataReader.findActiveInstruments()
            .collectList()
            .flatMap(instrumentList->importhandler.filterInstruments(instrumentList, dataReader.getSecurityMetricsKeyToTsMap()))
            .flatMapMany(Flux::fromIterable)
            .flatMap(i->importSecurityMetrics4Instrument(i));
    }

    public Mono<SecurityMetrics> importSecurityMetrics4InstrumentKey(String businesskey) {
        return dataReader.findByBusinesskey(businesskey)
            .flatMap(i->importSecurityMetrics4Instrument(i));
    }

    private Mono<SecurityMetrics> importSecurityMetrics4Instrument(Instrument instrument) {
        return Mono.just(importhandler.importSecurityMetrics(instrument))
            .flatMap(this::setCurrencykey)
            .flatMap(this::approveSecurityMetrics);
    }

    private Mono<SecurityMetrics> approveSecurityMetrics(SecurityMetrics securityMetrics) {
        securityMetrics.setLastUpdateTs(LocalDateTime.now());
        if(securityMetrics!=null){
            securityMetricsImportedEventHandler.sendSecurityMetricsUpdatedEvent(securityMetrics);
        }
        return Mono.just(securityMetrics);
    }

    private Mono<SecurityMetrics> setCurrencykey(SecurityMetrics securityMetrics) {
        return dataReader.findCurrencyByCurrencyCode(securityMetrics.getCurrencyCode())
            .switchIfEmpty(handleNotExistingInstrument(securityMetrics.getCurrencyCode()))
            .flatMap(currency -> {
                securityMetrics.setCurrencyKey(currency.getBusinesskey());
                return Mono.just(securityMetrics);
            });
    }

}
