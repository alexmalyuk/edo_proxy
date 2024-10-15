package org.nautilus;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.nautilus.service.ConfigService;
import org.nautilus.service.InnerRequestService;
import org.nautilus.service.JsonValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@WebServlet("/edo/*")
public class EdoProxyServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(EdoProxyServlet.class);
    private final InnerRequestService innerRequestService;
    private final JsonValidationService jsonValidationService = new JsonValidationService();

    public EdoProxyServlet(InnerRequestService innerRequestService) {
        this.innerRequestService = innerRequestService;
    }

    public EdoProxyServlet() {
        ConfigService configService = new ConfigService();
        String edoServiceUrl = configService.getProperty("edoService.url");
        String authHeader = configService.getProperty("edoService.authHeader");
        int requestTimeout = Integer.parseInt(configService.getProperty("edoService.requestTimeout"));

        this.innerRequestService = new InnerRequestService(edoServiceUrl, authHeader, requestTimeout);
        logger.info("EdoProxyServlet initialized with URL: {} requestTimeout: {}", edoServiceUrl, requestTimeout);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String pathInfo = req.getPathInfo();
        logger.info("Received GET request for path: {}", pathInfo);

        try {
            if (pathInfo.matches("(?i)^/task/(thanks|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$")) {
                HttpResponse innerResponse = innerRequestService.forwardGet(pathInfo);
                int statusCode = innerResponse.getStatusLine().getStatusCode();
                if (statusCode == HttpServletResponse.SC_OK) {
                    copyResponseProperties(innerResponse, resp);
                    logger.info("GET request successfully forwarded and response copied");
                    return;
                } else {
                    logger.warn("Received unexpected status code: {} for GET request to {}", statusCode, pathInfo);
                }
            } else {
                logger.warn("GET request path does not match expected pattern: {}", pathInfo);
            }
        } catch (Exception e) {
            logger.error("Exception while processing GET request for path: {}", pathInfo, e);
        }
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String pathInfo = req.getPathInfo();
        logger.info("Received POST request for path: {}", pathInfo);

        try {
            if ("/task/approve".equalsIgnoreCase(pathInfo)) {
                String requestBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                logger.debug("Request body: {}", requestBody);
                if (jsonValidationService.isValidJson(requestBody)) {
                    logger.info("JSON validation successful for POST request");
                    HttpResponse innerResponse = innerRequestService.forwardPost(pathInfo, requestBody);

                    int statusCode = innerResponse.getStatusLine().getStatusCode();
                    logger.info("POST response status from inner service: {}", statusCode);
                    if (statusCode == HttpServletResponse.SC_FOUND || statusCode == HttpServletResponse.SC_OK) {
                        copyResponseProperties(innerResponse, resp);
                        logger.info("POST request successfully forwarded and response copied");
                        return;
                    } else {
                        logger.warn("Invalid JSON in POST request body for path: {}", pathInfo);
                    }
                }
            } else {
                logger.warn("POST request path does not match expected pattern: {}", pathInfo);
            }
        } catch (Exception e) {
            logger.error("Exception while processing POST request for path: {}", pathInfo, e);
        }

        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private void copyResponseProperties(HttpResponse innerResponse, HttpServletResponse resp) throws IOException {

        try {
            int statusCode = innerResponse.getStatusLine().getStatusCode();
            logger.debug("Copying response status: {}", statusCode);
            resp.setStatus(statusCode);
            //resp.setContentType("text/html; charset=UTF-8");

            for (Header header : innerResponse.getAllHeaders()) {
                if ("authorization".equalsIgnoreCase(header.getName())) {
                    continue;
                }
                resp.setHeader(header.getName(), header.getValue());
                logger.debug("Copied header: {} = {}", header.getName(), header.getValue());
            }
            // Читання тіла відповіді
            try (InputStream inputStream = innerResponse.getEntity().getContent();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                StringBuilder responseBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }

                byte[] responseBytes = responseBody.toString().getBytes(StandardCharsets.UTF_8);
                resp.setContentLength(responseBytes.length);
                resp.getWriter().write(responseBody.toString());
                resp.getWriter().flush();
                logger.info("Response body copied successfully");

            } catch (IOException e) {
                logger.error("Failed to read response body", e);
                throw e;
            }

        } catch (Exception e) {
            logger.error("Failed to copy response properties", e);
            throw e;
        }
    }
}
