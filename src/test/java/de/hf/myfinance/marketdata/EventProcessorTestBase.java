package de.hf.myfinance.marketdata;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.events.out.PriceUpdateEventHandler;
import de.hf.myfinance.marketdata.events.out.SecurityMetricsImportedEventHandler;
import de.hf.myfinance.marketdata.importhandler.AlphavantageHandler;
import de.hf.myfinance.marketdata.importhandler.ImportHandler;
import de.hf.myfinance.marketdata.importhandler.PolygonHandler;
import de.hf.myfinance.marketdata.persistence.DataReaderImpl;
import de.hf.myfinance.marketdata.persistence.repositories.EndOfDayPricesRepository;
import de.hf.myfinance.marketdata.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.marketdata.service.MarketDataService;
import de.hf.myfinance.marketdata.webtools.WebRequest;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.testhelper.MongoDbTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@Import({TestChannelBinderConfiguration.class})
public class EventProcessorTestBase extends MongoDbTestBase {
    @Autowired
    InstrumentRepository instrumentRepository;

    @Autowired
    EndOfDayPricesRepository endOfDayPricesRepository;



    @Mock
    WebRequest webRequest;

    @Autowired
    DataReaderImpl dataReaderImpl;

    @Autowired 
    AuditService auditService;

    MarketDataService marketDataService;

    @Autowired
    private OutputDestination target;

    @Autowired
    PriceUpdateEventHandler priceUpdateEventHandler;

    @Autowired
    SecurityMetricsImportedEventHandler securityMetricsImportedEventHandler;

    @Autowired
    @Qualifier("saveInstrumentProcessor")
    protected Consumer<Event<String, Instrument>> saveInstrumentProcessor;

    @Autowired
    @Qualifier("saveEndOfDayPriceProcessor")
    protected Consumer<Event<String, EndOfDayPrices>> saveEndOfDayPriceProcessor;

    String instrumentProcessorBindingName = "saveInstrumentProcessor-in-0";
    String endOfDayPriceProcessorBindingName = "savePricesProcessor-in-0";


    @BeforeEach
    void setupDb() {
        ImportHandler alphavantageHandler = new AlphavantageHandler(webRequest, auditService, dataReaderImpl);
        ImportHandler polygonHandler = new PolygonHandler(webRequest, auditService, dataReaderImpl);
        marketDataService = new MarketDataService(dataReaderImpl, alphavantageHandler, polygonHandler, priceUpdateEventHandler, securityMetricsImportedEventHandler, auditService);

        instrumentRepository.deleteAll().block();
        endOfDayPricesRepository.deleteAll().block();
        purgeMessages(instrumentProcessorBindingName);
        purgeMessages(endOfDayPriceProcessorBindingName);
    }

    protected void purgeMessages(String bindingName) {
        getMessages(bindingName);
    }

    protected List<String> getMessages(String bindingName){
        List<String> messages = new ArrayList<>();
        boolean anyMoreMessages = true;

        while (anyMoreMessages) {
            Message<byte[]> message =
                    getMessage(bindingName);

            if (message == null) {
                anyMoreMessages = false;

            } else {
                messages.add(new String(message.getPayload()));
            }
        }
        return messages;
    }

    protected Message<byte[]> getMessage(String bindingName){
        try {
            return target.receive(0, bindingName);
        } catch (NullPointerException npe) {
            LOG.error("getMessage() received a NPE with binding = {}", bindingName);
            return null;
        }
    }

}