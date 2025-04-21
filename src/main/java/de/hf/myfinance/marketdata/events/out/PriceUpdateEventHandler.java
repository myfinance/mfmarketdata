package de.hf.myfinance.marketdata.events.out;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static de.hf.myfinance.event.Event.Type.CREATE;

@Component
public class PriceUpdateEventHandler {

    private final StreamBridge streamBridge;

    public PriceUpdateEventHandler(StreamBridge streamBridge){
        this.streamBridge = streamBridge;
    }

    public void sendPricesUpdatedEvent(EndOfDayPrices endOfDayPrices){
        sendMessage("pricesupdated-out-0",
                new Event(CREATE, endOfDayPrices.getInstrumentBusinesskey(), endOfDayPrices));
    }

    private void sendMessage(String bindingName, Event event) {
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}
