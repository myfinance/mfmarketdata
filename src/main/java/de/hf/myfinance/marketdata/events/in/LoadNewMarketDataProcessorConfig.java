package de.hf.myfinance.marketdata.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.service.MarketDataService;
import de.hf.myfinance.restmodel.MarketDataImportType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class LoadNewMarketDataProcessorConfig {

    private final MarketDataService marketDataService;
    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="loadNewMarketDataProcessorConfig_Event";

    public LoadNewMarketDataProcessorConfig(MarketDataService marketDataService, AuditService auditService) {
        this.marketDataService = marketDataService;
        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<MarketDataImportType, String>> loadNewMarketDataProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at "+ event.getEventCreatedAt(), Severity.INFO, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case START:
                    var importType = event.getKey();
                    var data = event.getData();
                    switch(importType){
                        case TIME_SERIES_WEEKLY:
                            if(data==null || data.equals("all")){
                                marketDataService.importAllTimeSeries().collectList().block();
                            }
                            else {
                                marketDataService.importTimeSeries4Instrument(data).block();
                            }
                            break;
                        case PREV_CLOSE:
                            break;
                        case SECURITYMETRICS:
                            if(data==null || data.equals("all")){
                                marketDataService.importSecurityMetrics().collectList().block();
                            }
                            else {
                                marketDataService.importSecurityMetrics4InstrumentKey(data).block();
                            }
                            break;
                        default:
                            String errorMessage = "Incorrect import type: " + importType + ", expected TIME_SERIES_WEEKLY, PREV_CLOSE or SecurityMetrics";
                            auditService.saveMessage(errorMessage, Severity.WARN, AUDIT_MSG_TYPE);
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