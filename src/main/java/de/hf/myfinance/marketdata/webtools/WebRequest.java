package de.hf.myfinance.marketdata.webtools;

import java.io.IOException;
import java.util.Map;

public interface WebRequest {
    String getRequest(String url) throws IOException;
    Map<String, Object> getJsonMapFromUrl(String url);
}
