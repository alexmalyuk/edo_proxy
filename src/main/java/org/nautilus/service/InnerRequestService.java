package org.nautilus.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class InnerRequestService {
    private static final Logger logger = LoggerFactory.getLogger(InnerRequestService.class);
    private final String edoServiceUrl;
    private final String authHeader;
    private final int requestTimeout;

    public InnerRequestService(String edoServiceUrl, String authHeader, int requestTimeout) {
        this.edoServiceUrl = edoServiceUrl;
        this.authHeader = authHeader;
        this.requestTimeout = requestTimeout;
        logger.info("InnerRequestService initialized with URL: {}", edoServiceUrl);
    }

    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(requestTimeout)
                .build();
        logger.debug("HttpClient created with connect timeout: {}", requestConfig.getConnectTimeout());
        return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    }

    public HttpResponse forwardGet(String path) throws IOException {
        logger.info("Executing GET request to: {}{}", edoServiceUrl, path);
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(edoServiceUrl + path);
            httpGet.setHeader("Authorization", authHeader);
            HttpResponse response = httpClient.execute(httpGet);
            logger.info("GET request completed with status: {}", response.getStatusLine().getStatusCode());
            return response;
        } catch (IOException e) {
            logger.error("GET request to {}{} failed", edoServiceUrl, path, e);
            throw e;
        }
    }

    public HttpResponse forwardPost(String path, String body) throws IOException {
        logger.info("Executing POST request to: " + edoServiceUrl + path);
        logger.debug("POST request body: " + body);
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost httpPost = new HttpPost(edoServiceUrl + path);
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            httpPost.setHeader("Authorization", authHeader);
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
            HttpResponse response = httpClient.execute(httpPost);
            logger.info("POST request completed with status: {}", response.getStatusLine().getStatusCode());
            return response;
        } catch (IOException e) {
            logger.error("POST request to {}{} failed", edoServiceUrl, path, e);
            throw e;
        }
    }
}
