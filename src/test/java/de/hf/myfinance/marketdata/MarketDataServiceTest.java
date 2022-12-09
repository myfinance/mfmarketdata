package de.hf.myfinance.marketdata;

import de.hf.myfinance.marketdata.importhandler.ImportHandler;
import de.hf.myfinance.marketdata.persistence.InstrumentMapper;
import de.hf.myfinance.marketdata.persistence.entities.InstrumentEntity;
import de.hf.myfinance.marketdata.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.marketdata.service.MarketDataService;
import de.hf.myfinance.marketdata.webtools.WebRequest;
import de.hf.myfinance.restmodel.AdditionalMaps;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.EndOfDayPrice;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@Import({TestChannelBinderConfiguration.class})
public class MarketDataServiceTest extends EventProcessorTestBase {



    @Autowired
    MarketDataService marketDataService;

    @Autowired
    InstrumentRepository instrumentRepository;

    @Autowired
    InstrumentMapper instrumentMapper;

    @Test
    void importPrices() {
        var equity = new InstrumentEntity();
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
}
