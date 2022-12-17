package de.hf.myfinance.marketdata;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.importhandler.ImportHandler;
import de.hf.myfinance.marketdata.persistence.InstrumentMapper;
import de.hf.myfinance.marketdata.persistence.entities.InstrumentEntity;
import de.hf.myfinance.marketdata.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.marketdata.service.MarketDataService;
import de.hf.myfinance.marketdata.webtools.WebRequest;
import de.hf.myfinance.restmodel.*;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@Import({TestChannelBinderConfiguration.class})
public class MarketDataServiceTest extends EventProcessorTestBase {

    @Autowired
    @Qualifier("saveEndOfDayPriceProcessor")
    protected Consumer<Event<String, EndOfDayPrices>> saveEndOfDayPriceProcessor;

    @Autowired
    MarketDataService marketDataService;

    @Autowired
    InstrumentRepository instrumentRepository;


    @Test
    void importPrices() {
        var equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0005140008@14");
        var propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0005140008");
        equity.setAdditionalProperties(propertyMap);
        var additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        var equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DBK", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();

        marketDataService.importData().collectList().block();
        var messages = getMessages("pricesupdated-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        assertEquals(equity.getBusinesskey(), data.get("instrumentBusinesskey"));
        var prices = (HashMap)data.get("prices");
        assertEquals(2, prices.size());
        assertTrue(prices.containsKey("2022-12-08"));
        assertEquals(147.78 , ((HashMap)prices.get("2022-12-08")).get("value"));
        assertEquals("EUR" , ((HashMap)prices.get("2022-12-08")).get("currencyKey"));
        assertTrue(prices.containsKey("2022-12-02"));
        assertEquals(148.67 , ((HashMap)prices.get("2022-12-02")).get("value"));
        assertEquals("EUR" , ((HashMap)prices.get("2022-12-02")).get("currencyKey"));
    }

    @Test
    void importPricesOnlyForActiveInstruments() {
        var equity = new InstrumentEntity();
        equity.setActive(false);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0005140008@14");
        var propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0005140008");
        equity.setAdditionalProperties(propertyMap);
        var additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        var equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DBK", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();

        marketDataService.importData().collectList().block();
        var messages = getMessages("pricesupdated-out-0");
        assertEquals(0, messages.size());

    }

    @Test
    void saveManualPrices() {
        var equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0005140008@14");
        var propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0005140008");
        equity.setAdditionalProperties(propertyMap);
        var additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        var equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DBK", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();

        var endOfDayPrices = new EndOfDayPrices();
        endOfDayPrices.setInstrumentBusinesskey("DE0005140008@14");
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100.0, "EUR");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var price2 = new EndOfDayPrice(150.0, "USD");
        pricemap.put(LocalDate.of(2022,12,3), price2);
        endOfDayPrices.setPrices(pricemap);

        marketDataService.savePrices(endOfDayPrices).block();
        var messages = getMessages("pricesupdated-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        assertEquals("DE0005140008@14", data.get("instrumentBusinesskey"));
        var prices = (HashMap)data.get("prices");
        assertEquals(2, prices.size());
        assertTrue(prices.containsKey("2022-12-01"));
        assertEquals(100.0 , ((HashMap)prices.get("2022-12-01")).get("value"));
        assertEquals("EUR" , ((HashMap)prices.get("2022-12-01")).get("currencyKey"));
        assertTrue(prices.containsKey("2022-12-03"));
        assertEquals(150.0 , ((HashMap)prices.get("2022-12-03")).get("value"));
        assertEquals("USD" , ((HashMap)prices.get("2022-12-03")).get("currencyKey"));
    }

    @Test
    void saveManualPrices4existing() {
        var equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0005140008@14");
        var propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0005140008");
        equity.setAdditionalProperties(propertyMap);
        var additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        var equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DBK", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();

        var endOfDayPrices = new EndOfDayPrices();
        endOfDayPrices.setInstrumentBusinesskey("DE0005140008@14");
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100.0, "EUR");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var price2 = new EndOfDayPrice(150.0, "USD");
        pricemap.put(LocalDate.of(2022,12,3), price2);
        endOfDayPrices.setPrices(pricemap);

        Event createEvent = new Event(Event.Type.CREATE, "DE0005140008@14", endOfDayPrices);
        saveEndOfDayPriceProcessor.accept(createEvent);

        var newEndOfDayPrices = new EndOfDayPrices();
        newEndOfDayPrices.setInstrumentBusinesskey("DE0005140008@14");
        var newPricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var newPrice = new EndOfDayPrice(120.0, "EUR");
        newPricemap.put(LocalDate.of(2022,12,4), newPrice);
        newEndOfDayPrices.setPrices(newPricemap);

        marketDataService.savePrices(newEndOfDayPrices).block();

        var messages = getMessages("pricesupdated-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        assertEquals("DE0005140008@14", data.get("instrumentBusinesskey"));
        var prices = (HashMap)data.get("prices");
        assertEquals(3, prices.size());
        assertTrue(prices.containsKey("2022-12-01"));
        assertEquals(100.0 , ((HashMap)prices.get("2022-12-01")).get("value"));
        assertEquals("EUR" , ((HashMap)prices.get("2022-12-01")).get("currencyKey"));
        assertTrue(prices.containsKey("2022-12-03"));
        assertEquals(150.0 , ((HashMap)prices.get("2022-12-03")).get("value"));
        assertEquals("USD" , ((HashMap)prices.get("2022-12-03")).get("currencyKey"));
        assertTrue(prices.containsKey("2022-12-04"));
        assertEquals(120.0 , ((HashMap)prices.get("2022-12-04")).get("value"));
        assertEquals("EUR" , ((HashMap)prices.get("2022-12-04")).get("currencyKey"));
    }
}
