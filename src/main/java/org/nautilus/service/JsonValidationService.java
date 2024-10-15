package org.nautilus.service;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class JsonValidationService {
    private static final Logger logger = LoggerFactory.getLogger(JsonValidationService.class);
    private final Schema schema;

    public JsonValidationService() {
        try (InputStream schemaStream = getClass().getResourceAsStream("/schema.json")) {
            if (schemaStream == null) {
                logger.error("Schema file not found");
                throw new RuntimeException("Schema file not found");
            }
            JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
            this.schema = SchemaLoader.load(jsonSchema);
            logger.info("JSON schema loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load JSON schema", e);
            throw new RuntimeException("Failed to load JSON schema", e);
        }
    }

    public boolean isValidJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            schema.validate(jsonObject);
            logger.info("JSON is valid");
            return true;
        } catch (Exception e) {
            logger.warn("JSON validation failed: " + e.getMessage());
            return false;
        }
    }
}
