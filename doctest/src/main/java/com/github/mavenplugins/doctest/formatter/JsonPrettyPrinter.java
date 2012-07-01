package com.github.mavenplugins.doctest.formatter;

import org.apache.http.HttpEntity;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonPrettyPrinter implements EntityFormatter {
    
    protected ObjectMapper mapper = new ObjectMapper();
    
    public String format(HttpEntity entity, byte[] responseData) throws Exception {
        return mapper.writer(new DefaultPrettyPrinter()).writeValueAsString(mapper.readTree(responseData));
    }
    
}
