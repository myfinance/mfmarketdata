package de.hf.myfinance.marketdata.events.in;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.service.MarketDataService;
import de.hf.myfinance.restmodel.EndOfDayPrices;

@Configuration
public class ValidateSinglePriceProcessorConfig {
    private final AuditService auditService;
    private final MarketDataService marketDataService;
    protected static final String AUDIT_MSG_TYPE="ValidateSinglePriceProcessorConfig_Event";

    public ValidateSinglePriceProcessorConfig(AuditService auditService, MarketDataService marketDataService) {
        this.auditService = auditService;
        this.marketDataService=marketDataService;
    }

    @Bean
    public Consumer<Event<String, EndOfDayPrices>> validateSinglePriceProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at "+ event.getEventCreatedAt(), Severity.INFO, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    var price = event.getData();
                    auditService.saveMessage("Create Price for Instrument with Key: "+ price.getInstrumentBusinesskey(), Severity.INFO, AUDIT_MSG_TYPE);
                    marketDataService.validateAndSavePrices(price).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE event";
                    auditService.saveMessage(errorMessage, Severity.WARN, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.INFO, AUDIT_MSG_TYPE);

        };
    }
}
