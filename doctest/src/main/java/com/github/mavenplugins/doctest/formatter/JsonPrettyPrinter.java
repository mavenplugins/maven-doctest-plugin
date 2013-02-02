package com.github.mavenplugins.doctest.formatter;

import org.apache.http.HttpEntity;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Formats the response as a pretty printed JSON.
 */
public class JsonPrettyPrinter implements EntityFormatter {
    
    /**
     * The JSON mapper (threadsafe) used to stringify the response.
     */
    protected ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Returns an empty string if the response data is empty.
     */
    public String format(HttpEntity entity, byte[] responseData) throws Exception {
        if (responseData != null && responseData.length > 0) {
            return mapper.writer(new DefaultPrettyPrinter()).writeValueAsString(mapper.readTree(responseData));
        }
        return "";
    }
    
}
