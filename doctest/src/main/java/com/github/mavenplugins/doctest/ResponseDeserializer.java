package com.github.mavenplugins.doctest;

/**
 * Turns a response into an object.
 */
public interface ResponseDeserializer {
    
    /**
     * Returns the object represented through the data.
     */
    Object deserialize(byte[] data, Class<?> valueType) throws Exception;
    
}