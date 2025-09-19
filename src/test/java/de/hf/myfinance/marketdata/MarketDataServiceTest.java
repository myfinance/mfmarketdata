package de.hf.myfinance.marketdata;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.importhandler.AlphavantageHandler;
import de.hf.myfinance.marketdata.persistence.entities.EndOfDayPricesEntity;
import de.hf.myfinance.marketdata.persistence.entities.InstrumentEntity;
import de.hf.myfinance.restmodel.*;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@Import({TestChannelBinderConfiguration.class})
public class MarketDataServiceTest extends EventProcessorTestBase {

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

        var reply = new HashMap<String, Object>();
        var timeseries = new HashMap<String, Object>();
        var pricemap = new HashMap<String, String>();
        pricemap.put("4. close","238.8100");
        timeseries.put("2025-04-17",pricemap);
        var pricemapDay2 = new HashMap<String, String>();
        pricemapDay2.put("4. close","239.8100");
        timeseries.put("2025-04-18",pricemapDay2);
        reply.put("Weekly Time Series", timeseries) ;

        when(webRequestMock.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DBK"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        marketDataService.importAllTimeSeries().collectList().block();
        var messages = getMessages("pricesupdated-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        assertEquals(equity.getBusinesskey(), data.get("instrumentBusinesskey"));
        var prices = (HashMap)data.get("prices");
        assertEquals(2, prices.size());
        assertTrue(prices.containsKey("2025-04-17"));
        assertEquals(238.81 , ((HashMap)prices.get("2025-04-17")).get("value"));
        assertEquals("EUR" , ((HashMap)prices.get("2025-04-17")).get("currencyKey"));
        assertTrue(prices.containsKey("2025-04-18"));
        assertEquals(239.81 , ((HashMap)prices.get("2025-04-18")).get("value"));
        assertEquals("EUR" , ((HashMap)prices.get("2025-04-18")).get("currencyKey"));
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

        @Test
    void importPolygonPrices() {
        var equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0005140008@14");
        var propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0005140008");
        equity.setAdditionalProperties(propertyMap);
        var additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        var equitySymbols = new HashMap<String, String>();
        equitySymbols.put("HOOD", "USD");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();

        var reply = new HashMap<String, Object>();
        var timeseries = new HashMap<String, Object>();
        var pricemap = new HashMap<String, String>();
        pricemap.put("4. close","238.8100");
        timeseries.put("2025-04-17",pricemap);
        var pricemapDay2 = new HashMap<String, String>();
        pricemapDay2.put("4. close","239.8100");
        timeseries.put("2025-04-18",pricemapDay2);
        reply.put("Weekly Time Series", timeseries) ;


        //when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DBK"+AlphavantageHandler.EQ_URLPOSTFIX))
        //.thenReturn(reply);

        marketDataService.importPrevClose4Instrument("DE0005140008@14").block();
        var messages = getMessages("pricesupdated-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        assertEquals(equity.getBusinesskey(), data.get("instrumentBusinesskey"));
        var prices = (HashMap)data.get("prices");
        assertEquals(1, prices.size());
        double price = (Double) ((HashMap)prices.values().toArray()[0]).get("value");
        assertEquals(118.64,price);
    }
}
