package de.hf.myfinance.marketdata.webtools;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Map;

@Component("WebRequestImpl")
@ConditionalOnProperty(name = "marketdata.isTest", havingValue = "false", matchIfMissing = true)
public class WebRequestImpl implements WebRequest {

    @Value("${marketdata.webrequest.useproxy:false}") boolean useProxy;
    @Value("${marketdata.webrequest.proxyurl:empty}") String proxyUrl;
    @Value("${marketdata.webrequest.proxyport:0}") int proxyPort;
    @Value("${marketdata.webrequest.proxyuser:empty}")String proxyUser;
    @Value("${marketdata.webrequest.proxypw:empty}")String proxyPw;
    @Value("${marketdata.webrequest.timeout:0}") int timeout;


    public String getRequest(String url) throws IOException {

        InputStreamReader inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuilder responseBuilder;

        URLConnection connection = getUrlConnection(url);

        return getResponse(connection).toString();
    }

    private StringBuilder getResponse(URLConnection connection) throws IOException {
        StringBuilder responseBuilder;
        InputStreamReader inputStream = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = new InputStreamReader(connection.getInputStream(), "UTF-8");
            bufferedReader = new BufferedReader(inputStream);
            responseBuilder = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                responseBuilder.append(line);
            }

        } finally {
            if(inputStream!=null) inputStream.close();
            if(bufferedReader!=null) bufferedReader.close();
        }
        return responseBuilder;
    }

    private URLConnection getUrlConnection(String url) throws IOException {
        URL request = new URL(url);
        URLConnection connection;

        if(useProxy) {

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl, proxyPort));

            Authenticator authenticator = new Authenticator() {

                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(proxyUser, proxyPw.toCharArray()));
                }
            };
            Authenticator.setDefault(authenticator);

            connection = request.openConnection(proxy);

        } else {
            connection = request.openConnection();
        }

        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        return connection;
    }

    public Map<String, Object> getJsonMapFromUrl(String url) {
        String returnvalue;
        try {
            returnvalue=getRequest(url);
        } catch (IOException e) {
            throw new MFException(MFMsgKey.NO_RESPONSE_FROM_URL_EXCEPTION, "no or empty response form "+url, e);
        }
        if(returnvalue== null || returnvalue.isEmpty()){
            throw new MFException(MFMsgKey.NO_RESPONSE_FROM_URL_EXCEPTION, "empty response form "+url);
        }
        Gson gson = new Gson();

        return gson.fromJson(returnvalue, new TypeToken<Map<String, Object>>(){}.getType());
    }
}
