package de.hf.myfinance.marketdata;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.persistence.repositories.EndOfDayPricesRepository;
import de.hf.myfinance.marketdata.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.testhelper.MongoDbTestBase;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private OutputDestination target;

    @Autowired
    @Qualifier("saveInstrumentProcessor")
    protected Consumer<Event<String, Instrument>> saveInstrumentProcessor;

    String instrumentProcessorBindingName = "saveInstrumentProcessor-in-0";
    String endOfDayPriceProcessorBindingName = "savePricesProcessor-in-0";


    @BeforeEach
    void setupDb() {
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
