package de.hf.myfinance.marketdata.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.persistence.InstrumentMapper;
import de.hf.myfinance.marketdata.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class SaveInstrumentProcessorConfig {

    private final InstrumentMapper instrumentMapper;
    private final InstrumentRepository instrumentRepository;
    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="SaveInstrumentProcessorConfig_Event";

    @Autowired
    public SaveInstrumentProcessorConfig(InstrumentMapper instrumentMapper, InstrumentRepository instrumentRepository, AuditService auditService) {
        this.instrumentMapper = instrumentMapper;
        this.instrumentRepository = instrumentRepository;
        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<String, Instrument>> saveInstrumentProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at "+ event.getEventCreatedAt(), Severity.INFO, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    Instrument instrument = event.getData();
                    auditService.saveMessage("Create instrument with ID: "+ instrument.getBusinesskey(), Severity.INFO, AUDIT_MSG_TYPE);
                    if(instrument.getInstrumentType().equals(InstrumentType.CURRENCY) || instrument.getInstrumentType().equals(InstrumentType.EQUITY)){
                        var instrumentEntity = instrumentMapper.apiToEntity(instrument);
                        instrumentRepository.deleteByBusinesskey(instrumentEntity.getBusinesskey()).then(instrumentRepository.save(instrumentEntity)).block();
                    }
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE event";
                    auditService.saveMessage(errorMessage, Severity.WARN, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.INFO, AUDIT_MSG_TYPE);

        };
    }
}