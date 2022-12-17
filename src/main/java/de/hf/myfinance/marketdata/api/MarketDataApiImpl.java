package de.hf.myfinance.marketdata.api;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.service.MarketDataService;
import de.hf.myfinance.restapi.MarketDataApi;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RestController;

import de.hf.framework.utils.ServiceUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import static de.hf.myfinance.event.Event.Type.START;

@RestController
public class MarketDataApiImpl implements MarketDataApi {
    ServiceUtil serviceUtil;
    private final StreamBridge streamBridge;
    private final Scheduler publishEventScheduler;
    private final MarketDataService marketDataService;

    @Autowired
    public MarketDataApiImpl(ServiceUtil serviceUtil, StreamBridge streamBridge, MarketDataService marketDataService, @Qualifier("publishEventScheduler") Scheduler publishEventScheduler) {
        this.serviceUtil = serviceUtil;
        this.streamBridge = streamBridge;
        this.publishEventScheduler = publishEventScheduler;
        this.marketDataService = marketDataService;
    }

    @Override
    public String index() {
        return "Hello my marketDataService";
    }

    @Override
    public Mono<String> loadNewMarketData(){
        return Mono.fromCallable(() -> {

            sendMessage("loadNewMarketData-out-0",
                    new Event(START, "load", null));
            return "MarketData loading started:";
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> savePrices(EndOfDayPrices endOfDayPrices) {
        return marketDataService.savePrices(endOfDayPrices);
    }

    @Override
    public Mono<EndOfDayPrices> getEndOfDayPrices(String businesskey) {
        return marketDataService.getEndOfDayPrices(businesskey);
    }

    /**
     * Since the sendMessage() uses blocking code, when calling streamBridge,
     * it has to be executed on a thread provided by a dedicated scheduler, publishEventScheduler
     */
    private void sendMessage(String bindingName, Event event) {
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }

}