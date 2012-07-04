package com.github.mavenplugins.doctest.formatter;

import org.apache.http.HttpEntity;

/**
 * An EntityFormatter is responsible for formatting a response.
 */
public interface EntityFormatter {
    
    /**
     * Actually formats the given responseData.
     * 
     * @param entity The entity where the response came from.
     * @param responseData The raw response data - some Entity-objects doesn't support multiple read operations.
     */
    String format(HttpEntity entity, byte[] responseData) throws Exception;
    
}
