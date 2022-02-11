package de.hf.myfinance.marketdata.api;

import de.hf.myfinance.restapi.MarketDataApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import de.hf.framework.utils.ServiceUtil;

@RestController
public class MarketDataApiImpl implements MarketDataApi {
    ServiceUtil serviceUtil;

    @Autowired
    public MarketDataApiImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public String index() {
        return "Hello my marketDataService";
    }

}