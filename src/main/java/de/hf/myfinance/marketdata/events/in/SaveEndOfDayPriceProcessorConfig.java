package de.hf.myfinance.marketdata.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.persistence.EndOfDayPricesMapper;
import de.hf.myfinance.marketdata.persistence.repositories.EndOfDayPricesRepository;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class SaveEndOfDayPriceProcessorConfig {
    private final AuditService auditService;
    private final EndOfDayPricesMapper endOfDayPricesMapper;
    private final EndOfDayPricesRepository endOfDayPricesRepository;
    protected static final String AUDIT_MSG_TYPE="SaveEndOfDayPriceProcessorConfig_Event";

    @Autowired
    public SaveEndOfDayPriceProcessorConfig(EndOfDayPricesMapper endOfDayPricesMapper, EndOfDayPricesRepository endOfDayPricesRepository, AuditService auditService) {
        this.endOfDayPricesMapper = endOfDayPricesMapper;
        this.endOfDayPricesRepository = endOfDayPricesRepository;
        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<String, EndOfDayPrices>> saveEndOfDayPriceProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at "+ event.getEventCreatedAt(), Severity.INFO, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    EndOfDayPrices endOfDayPrices = event.getData();
                    auditService.saveMessage("Create EndOfDayPrices for Instrument: "+ endOfDayPrices.getInstrumentBusinesskey(), Severity.INFO, AUDIT_MSG_TYPE);
                    var endOfDayPricesEntity = endOfDayPricesMapper.apiToEntity(endOfDayPrices);
                    endOfDayPricesRepository.deleteByInstrumentBusinesskey(endOfDayPricesEntity.getInstrumentBusinesskey())
                            .then(endOfDayPricesRepository.save(endOfDayPricesEntity)).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE event";
                    auditService.saveMessage(errorMessage, Severity.WARN, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.INFO, AUDIT_MSG_TYPE);

        };
    }
}