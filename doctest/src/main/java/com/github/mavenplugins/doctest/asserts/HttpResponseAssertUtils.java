package com.github.mavenplugins.doctest.asserts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

/**
 * This is an utility class used to check http responses.
 */
public class HttpResponseAssertUtils {
    
    /**
     * Checks the response for the existing of the specified header.
     * 
     * @param response the response
     * @param header the string chuck
     */
    public static void assertHeaderExists(HttpResponse response, String header) {
        assertHeaderExists("expected header '" + header + '\'', response, header);
    }
    
    /**
     * Checks the response for the existing of the specified header.
     * 
     * @param response the response
     * @param header the string chuck
     * @param message the assert message
     */
    public static void assertHeaderExists(String message, HttpResponse response, String header) {
        assertTrue(message, response.containsHeader(header));
    }
    
    /**
     * Checks the response for the containment of the specified header and value.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     */
    public static void assertHeaderContains(HttpResponse response, String header, String value) {
        assertHeaderContains("expected header '" + header + "' with value '" + value + '\'', response, header, value, 0);
    }
    
    /**
     * Checks the response for the containment of the specified header and value.
     * 
     * @param response the response
     * @param header the string chuck
     * @param message the assert message
     * @param value the value the header should contain
     */
    public static void assertHeaderContains(String message, HttpResponse response, String header, String value) {
        assertHeaderContains(message, response, header, value, 0);
    }
    
    /**
     * Checks the response for the containment of the specified header and value at the specified index.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param index the index of the header
     */
    public static void assertHeaderContains(HttpResponse response, String header, String value, int index) {
        assertHeaderContains("expected header '" + header + "' with value '" + value + "' at index " + index, response, header, value, index);
    }
    
    /**
     * Checks the response for the containment of the specified header and value at the specified index.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param index the index of the header
     * @param message the assert message
     */
    public static void assertHeaderContains(String message, HttpResponse response, String header, String value, int index) {
        Header[] headers = response.getHeaders(header);
        
        assertHeaderExists(message, response, header);
        assertTrue(message, Pattern.compile(value).matcher(headers[index].getValue()).find());
    }
    
    /**
     * Checks the response for the equality of the specified header and value.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     */
    public static void assertHeaderEquals(HttpResponse response, String header, String value) {
        assertHeaderEquals("expected header '" + header + "' with value '" + value + '\'', response, header, value, 0);
    }
    
    /**
     * Checks the response for the equality of the specified header and value.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param message the assert message
     */
    public static void assertHeaderEquals(String message, HttpResponse response, String header, String value) {
        assertHeaderEquals(message, response, header, value, 0);
    }
    
    /**
     * Checks the response for the equality of the specified header and value at the specified index.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param index the index of the header
     */
    public static void assertHeaderEquals(HttpResponse response, String header, String value, int index) {
        assertHeaderEquals("expected header '" + header + "' with value '" + value + "' at index " + index, response, header, value, index);
    }
    
    /**
     * Checks the response for the equality of the specified header and value at the specified index.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param message the assert message
     * @param index the index of the header
     */
    public static void assertHeaderEquals(String message, HttpResponse response, String header, String value, int index) {
        Header[] headers = response.getHeaders(header);
        
        assertHeaderExists(message, response, header);
        assertEquals(message, value, headers[index].getValue());
    }
    
    /**
     * Checks the response for the equality of the specified header and value.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     */
    public static void assertHeaderEqualsIgnoresCase(HttpResponse response, String header, String value) {
        assertHeaderEqualsIgnoresCase("expected header '" + header + "' with value '" + value + '\'', response, header, value, 0);
    }
    
    /**
     * Checks the response for the equality of the specified header and value.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param message the assert message
     */
    public static void assertHeaderEqualsIgnoresCase(String message, HttpResponse response, String header, String value) {
        assertHeaderEqualsIgnoresCase(message, response, header, value, 0);
    }
    
    /**
     * Checks the response for the equality of the specified header and value at the specified index.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param index the index of the header
     */
    public static void assertHeaderEqualsIgnoresCase(HttpResponse response, String header, String value, int index) {
        assertHeaderEqualsIgnoresCase("expected header '" + header + "' with value '" + value + "' at index " + index, response, header, value, index);
    }
    
    /**
     * Checks the response for the equality of the specified header and value at the specified index.
     * 
     * @param response the response
     * @param header the string chuck
     * @param value the value the header should contain
     * @param message the assert message
     * @param index the index of the header
     */
    public static void assertHeaderEqualsIgnoresCase(String message, HttpResponse response, String header, String value, int index) {
        Header[] headers = response.getHeaders(header);
        
        assertHeaderExists(message, response, header);
        assertTrue(message, headers[index].getValue().equals(value));
    }
    
}
