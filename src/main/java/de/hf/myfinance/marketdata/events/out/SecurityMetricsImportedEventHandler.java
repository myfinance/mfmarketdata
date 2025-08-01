package de.hf.myfinance.marketdata.events.out;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.SecurityMetrics;

import static de.hf.myfinance.event.Event.Type.CREATE;

@Component
public class SecurityMetricsImportedEventHandler {
        private final StreamBridge streamBridge;

    public SecurityMetricsImportedEventHandler(StreamBridge streamBridge){
        this.streamBridge = streamBridge;
    }

    public void sendSecurityMetricsUpdatedEvent(SecurityMetrics securityMetrics){
        sendMessage("securitymetrics-updated-out-0",
                new Event(CREATE, securityMetrics.getBusinesskey(), securityMetrics));
    }

    private void sendMessage(String bindingName, Event event) {
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}
