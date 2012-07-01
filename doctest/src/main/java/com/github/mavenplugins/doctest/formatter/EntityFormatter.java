package com.github.mavenplugins.doctest.formatter;

import org.apache.http.HttpEntity;

public interface EntityFormatter {
    
    String format(HttpEntity entity, byte[] responseData) throws Exception;
    
}
