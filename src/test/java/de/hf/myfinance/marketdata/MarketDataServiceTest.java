package de.hf.myfinance.marketdata;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.marketdata.importhandler.AlphavantageHandler;
import de.hf.myfinance.marketdata.persistence.KeyTsProjection;
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

        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DBK"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        marketDataService.importData().collectList().block();
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

    /**
     * import prices for active instruments with symbol. if more then 10 then take instrument with no or oldest prices
     */
    @Test
    void importPricesRuleTest() {

        var reply = new HashMap<String, Object>();
        var timeseries = new HashMap<String, Object>();
        var pricemap = new HashMap<String, String>();
        pricemap.put("4. close","238.8100");
        timeseries.put("2025-04-17",pricemap);
        reply.put("Weekly Time Series", timeseries) ;

        //inactive instrument will get no price
        var equity = new InstrumentEntity();
        equity.setActive(false);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000001@14");
        var propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000001");
        equity.setAdditionalProperties(propertyMap);
        var additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        var equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB1", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();

        //instrument without Symbol will get no price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000002@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000002");
        equity.setAdditionalProperties(propertyMap);
        instrumentRepository.save(equity).block();

        //1. valid instrument with no existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000003@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000003");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB2", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DB2"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        //2. valid instrument with no existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000004@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000004");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB4", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DB4"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        //3. valid instrument with no existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000005@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000005");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB5", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DB5"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        //4. valid instrument with no existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000006@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000006");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB6", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DB6"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        //5. valid instrument with no existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000007@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000007");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB7", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DB7"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        //6. valid instrument with no existing price: should not get a new price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000008@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000008");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB8", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DB8"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);

        //1. valid instrument with existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000009@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000009");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("DB9", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"DB9"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);
        var endOfDayPrices = new EndOfDayPricesEntity();
        endOfDayPrices.setInstrumentBusinesskey("DE0000000009@14");
        var prices = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100.0, "EUR");
        prices.put(LocalDate.of(2022,12,1), price);
        endOfDayPrices.setPrices(prices);
        endOfDayPrices.setLastUpdateTs(LocalDateTime.of(2024,1,1,12,12));
        endOfDayPricesRepository.save(endOfDayPrices).block();

        //2. valid instrument with existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000010@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000010");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("D10", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"D10"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);
        endOfDayPrices = new EndOfDayPricesEntity();
        endOfDayPrices.setInstrumentBusinesskey("DE0000000010@14");
        prices = new HashMap<LocalDate, EndOfDayPrice>();
        price = new EndOfDayPrice(100.0, "EUR");
        prices.put(LocalDate.of(2022,12,1), price);
        endOfDayPrices.setPrices(prices);
        endOfDayPrices.setLastUpdateTs(LocalDateTime.of(2024,1,2,12,12));
        endOfDayPricesRepository.save(endOfDayPrices).block();

        //3. valid instrument with existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000011@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000011");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("D11", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"D11"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);
        endOfDayPrices = new EndOfDayPricesEntity();
        endOfDayPrices.setInstrumentBusinesskey("DE0000000011@14");
        prices = new HashMap<LocalDate, EndOfDayPrice>();
        price = new EndOfDayPrice(100.0, "EUR");
        prices.put(LocalDate.of(2022,12,1), price);
        endOfDayPrices.setPrices(prices);
        endOfDayPrices.setLastUpdateTs(LocalDateTime.of(2024,1,3,12,12));
        endOfDayPricesRepository.save(endOfDayPrices).block();

        //4. valid instrument with existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000012@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000012");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("D12", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"D12"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);
        endOfDayPrices = new EndOfDayPricesEntity();
        endOfDayPrices.setInstrumentBusinesskey("DE0000000012@14");
        prices = new HashMap<LocalDate, EndOfDayPrice>();
        price = new EndOfDayPrice(100.0, "EUR");
        prices.put(LocalDate.of(2022,12,4), price);
        endOfDayPrices.setPrices(prices);
        endOfDayPrices.setLastUpdateTs(LocalDateTime.of(2024,1,4,12,12));
        endOfDayPricesRepository.save(endOfDayPrices).block();

        //5. valid instrument with existing price
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000013@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000013");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("D13", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"D13"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);
        endOfDayPrices = new EndOfDayPricesEntity();
        endOfDayPrices.setInstrumentBusinesskey("DE0000000013@14");
        prices = new HashMap<LocalDate, EndOfDayPrice>();
        price = new EndOfDayPrice(100.0, "EUR");
        prices.put(LocalDate.of(2022,12,4), price);
        endOfDayPrices.setPrices(prices);
        endOfDayPrices.setLastUpdateTs(LocalDateTime.of(2024,1,5,12,12));
        endOfDayPricesRepository.save(endOfDayPrices).block();

        //6. valid instrument with existing price : should not get a new price because its last price was to new
        equity = new InstrumentEntity();
        equity.setActive(true);
        equity.setInstrumentType(InstrumentType.EQUITY);
        equity.setBusinesskey("DE0000000014@14");
        propertyMap = new HashMap<AdditionalProperties, String>();
        propertyMap.put(AdditionalProperties.ISIN, "DE0000000014");
        equity.setAdditionalProperties(propertyMap);
        additionalMaps = new HashMap<AdditionalMaps, Map<String, String>>();
        equitySymbols = new HashMap<String, String>();
        equitySymbols.put("D14", "EUR");
        additionalMaps.put(AdditionalMaps.EQUITYSYMBOLS, equitySymbols);
        equity.setAdditionalMaps(additionalMaps);
        instrumentRepository.save(equity).block();
        when(webRequest.getJsonMapFromUrl(AlphavantageHandler.EQ_URLPREFIX+"D14"+AlphavantageHandler.EQ_URLPOSTFIX))
        .thenReturn(reply);
        endOfDayPrices = new EndOfDayPricesEntity();
        endOfDayPrices.setInstrumentBusinesskey("DE0000000014@14");
        prices = new HashMap<LocalDate, EndOfDayPrice>();
        price = new EndOfDayPrice(100.0, "EUR");
        prices.put(LocalDate.of(2022,12,4), price);
        endOfDayPrices.setPrices(prices);
        endOfDayPrices.setLastUpdateTs(LocalDateTime.of(2024,1,6,12,12));
        endOfDayPricesRepository.save(endOfDayPrices).block();

        marketDataService.importData().collectList().block();
        var messages = getMessages("pricesupdated-out-0");
        assertEquals(10, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var keyList = new ArrayList<String>();
        for(int i =0; i<10; i++){
            var data = (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(i))).get("data");
            keyList.add((String)data.get("instrumentBusinesskey"));
        }
        assertTrue(keyList.contains("DE0000000003@14"));
        assertTrue(keyList.contains("DE0000000004@14"));
        assertTrue(keyList.contains("DE0000000005@14"));
        assertTrue(keyList.contains("DE0000000006@14"));
        assertTrue(keyList.contains("DE0000000007@14"));
        assertTrue(keyList.contains("DE0000000009@14"));
        assertTrue(keyList.contains("DE0000000010@14"));
        assertTrue(keyList.contains("DE0000000011@14"));
        assertTrue(keyList.contains("DE0000000012@14"));
        assertTrue(keyList.contains("DE0000000013@14"));


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
