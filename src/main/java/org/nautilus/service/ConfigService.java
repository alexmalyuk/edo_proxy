package org.nautilus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private final Properties properties;

    public ConfigService() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Unable to find config.properties");
                return;
            }
            properties.load(input);
            logger.info("config.properties loaded successfully");
        } catch (IOException ex) {
            logger.error("Error loading config.properties", ex);
        }
    }

    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.warn("Property not found: {}", key);
        } else {
            logger.debug("Retrieved property {} with value: {}", key, value);
        }
        return value;
    }
}
