package com.github.mavenplugins.doctest.asserts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

public class HttpResponseAssertUtils {
    
    public static void assertHeaderExists(HttpResponse response, String header) {
        assertHeaderExists(null, response, header);
    }
    
    public static void assertHeaderExists(String message, HttpResponse response, String header) {
        assertTrue(message, response.containsHeader(header));
    }
    
    public static void assertHeaderContains(HttpResponse response, String header, String value) {
        assertHeaderContains(null, response, header, value, 0);
    }
    
    public static void assertHeaderContains(String message, HttpResponse response, String header, String value) {
        assertHeaderContains(message, response, header, value, 0);
    }
    
    public static void assertHeaderContains(HttpResponse response, String header, String value, int index) {
        assertHeaderContains(null, response, header, value, index);
    }
    
    public static void assertHeaderContains(String message, HttpResponse response, String header, String value, int index) {
        Header[] headers = response.getHeaders(header);
        
        assertHeaderExists(message, response, header);
        assertTrue(message, Pattern.compile(value).matcher(headers[index].getValue()).find());
    }
    
    public static void assertHeaderEquals(HttpResponse response, String header, String value) {
        assertHeaderEquals(null, response, header, value, 0);
    }
    
    public static void assertHeaderEquals(String message, HttpResponse response, String header, String value) {
        assertHeaderEquals(message, response, header, value, 0);
    }
    
    public static void assertHeaderEquals(HttpResponse response, String header, String value, int index) {
        assertHeaderEquals(null, response, header, value, index);
    }
    
    public static void assertHeaderEquals(String message, HttpResponse response, String header, String value, int index) {
        Header[] headers = response.getHeaders(header);
        
        assertHeaderExists(message, response, header);
        assertEquals(message, value, headers[index].getValue());
    }
    
    public static void assertHeaderEqualsIgnoresCase(HttpResponse response, String header, String value) {
        assertHeaderEqualsIgnoresCase(null, response, header, value, 0);
    }
    
    public static void assertHeaderEqualsIgnoresCase(String message, HttpResponse response, String header, String value) {
        assertHeaderEqualsIgnoresCase(message, response, header, value, 0);
    }
    
    public static void assertHeaderEqualsIgnoresCase(HttpResponse response, String header, String value, int index) {
        assertHeaderEqualsIgnoresCase(null, response, header, value, index);
    }
    
    public static void assertHeaderEqualsIgnoresCase(String message, HttpResponse response, String header, String value, int index) {
        Header[] headers = response.getHeaders(header);
        
        assertHeaderExists(message, response, header);
        assertTrue(message, headers[index].getValue().equals(value));
    }
    
}
