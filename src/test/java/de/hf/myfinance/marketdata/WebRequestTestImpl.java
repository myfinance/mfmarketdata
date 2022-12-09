package de.hf.myfinance.marketdata;

import de.hf.myfinance.marketdata.importhandler.AlphavantageHandler;
import de.hf.myfinance.marketdata.webtools.WebRequestImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component("WebRequestTestImpl")
@ConditionalOnProperty(name = "marketdata.isTest", havingValue = "true")
public class WebRequestTestImpl extends WebRequestImpl {
    @Override
    public String getRequest(String url) throws IOException {

        if(url.startsWith(AlphavantageHandler.EQ_URLPREFIX)){
            return "// 20221209111926\n" +
                    "// https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol=IBM&apikey=demo\n" +
                    "\n" +
                    "{\n" +
                    "  \"Meta Data\": {\n" +
                    "    \"1. Information\": \"Weekly Prices (open, high, low, close) and Volumes\",\n" +
                    "    \"2. Symbol\": \"IBM\",\n" +
                    "    \"3. Last Refreshed\": \"2022-12-08\",\n" +
                    "    \"4. Time Zone\": \"US/Eastern\"\n" +
                    "  },\n" +
                    "  \"Weekly Time Series\": {\n" +
                    "    \"2022-12-08\": {\n" +
                    "      \"1. open\": \"147.9400\",\n" +
                    "      \"2. high\": \"149.1530\",\n" +
                    "      \"3. low\": \"146.2900\",\n" +
                    "      \"4. close\": \"147.7800\",\n" +
                    "      \"5. volume\": \"12269349\"\n" +
                    "    },\n" +
                    "    \"2022-12-02\": {\n" +
                    "      \"1. open\": \"147.9800\",\n" +
                    "      \"2. high\": \"150.0100\",\n" +
                    "      \"3. low\": \"145.6700\",\n" +
                    "      \"4. close\": \"148.6700\",\n" +
                    "      \"5. volume\": \"20066941\"\n" +
                    "    }}}";
        } else
        if(url.startsWith(AlphavantageHandler.FX_URLPREFIX)){
            return "{\n" +
                    "  \"Meta Data\": {\n" +
                    "    \"1. Information\": \"Forex Daily Prices (open, high, low, close)\",\n" +
                    "    \"2. From Symbol\": \"EUR\",\n" +
                    "    \"3. To Symbol\": \"USD\",\n" +
                    "    \"4. Output Size\": \"Compact\",\n" +
                    "    \"5. Last Refreshed\": \"2022-12-09 10:25:00\",\n" +
                    "    \"6. Time Zone\": \"UTC\"\n" +
                    "  },\n" +
                    "  \"Time Series FX (Daily)\": {\n" +
                    "    \"2022-12-09\": {\n" +
                    "      \"1. open\": \"1.05566\",\n" +
                    "      \"2. high\": \"1.05882\",\n" +
                    "      \"3. low\": \"1.05450\",\n" +
                    "      \"4. close\": \"1.05607\"\n" +
                    "    },\n" +
                    "    \"2022-12-08\": {\n" +
                    "      \"1. open\": \"1.05048\",\n" +
                    "      \"2. high\": \"1.05649\",\n" +
                    "      \"3. low\": \"1.04880\",\n" +
                    "      \"4. close\": \"1.05540\"\n" +
                    "    }}}";
        }
        return "";
    }
}
