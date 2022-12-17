package de.hf.myfinance.marketdata.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.service.MarketDataService;
import de.hf.myfinance.restmodel.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class loadNewMarketDataProcessorConfig {

    private final MarketDataService marketDataService;
    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="loadNewMarketDataProcessorConfig_Event";

    @Autowired
    public loadNewMarketDataProcessorConfig(MarketDataService marketDataService, AuditService auditService) {
        this.marketDataService = marketDataService;
        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<String, Instrument>> loadNewMarketDataProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at "+ event.getEventCreatedAt(), Severity.INFO, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case START:
                    marketDataService.importData().collectList().block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE event";
                    auditService.saveMessage(errorMessage, Severity.WARN, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.INFO, AUDIT_MSG_TYPE);

        };
    }
}