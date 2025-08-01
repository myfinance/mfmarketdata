package de.hf.myfinance.marketdata.events.in;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.persistence.SecurityMetricsMapper;
import de.hf.myfinance.marketdata.persistence.repositories.SecurityMetricsRepository;
import de.hf.myfinance.restmodel.SecurityMetrics;

@Configuration
public class SaveSecurityMetricsProcessorConfig {
    private final SecurityMetricsMapper securityMetricsMapper;
    private final SecurityMetricsRepository securityMetricRepository;
    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="SaveInstrumentProcessorConfig_Event";

    public SaveSecurityMetricsProcessorConfig(SecurityMetricsMapper securityMetricsMapper, SecurityMetricsRepository securityMetricRepository, AuditService auditService) {
        this.securityMetricsMapper = securityMetricsMapper;
        this.securityMetricRepository = securityMetricRepository;
        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<String, SecurityMetrics>> saveSecurityMetricsProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at "+ event.getEventCreatedAt(), Severity.INFO, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    SecurityMetrics  securityMetrics = event.getData();
                    auditService.saveMessage("Create securityMetrics with ID: "+ securityMetrics.getBusinesskey(), Severity.INFO, AUDIT_MSG_TYPE);
                    var entity = securityMetricsMapper.apiToEntity(securityMetrics);
                    securityMetricRepository.deleteByInstrumentBusinesskey(entity.getBusinesskey()).then(securityMetricRepository.save(entity)).block();

                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE event";
                    auditService.saveMessage(errorMessage, Severity.WARN, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.INFO, AUDIT_MSG_TYPE);

        };
    }
}