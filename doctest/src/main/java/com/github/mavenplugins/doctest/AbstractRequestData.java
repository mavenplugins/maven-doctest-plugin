package com.github.mavenplugins.doctest;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An abstract implementation of a request configuration.
 */
public abstract class AbstractRequestData implements RequestData {
    
    /**
     * Gets a null back.
     */
    public HttpParams getParameters() {
        return null;
    }
    
    /**
     * Gets a "GET".
     */
    public String getMethod() {
        return HttpGet.METHOD_NAME;
    }
    
    /**
     * Gets a null back.
     */
    public Header[] getHeaders() {
        return null;
    }
    
    /**
     * Gets a null back.
     */
    public HttpEntity getHttpEntity() {
        return null;
    }
    
    /**
     * Gets a null back.
     */
    public Credentials getCredentials() {
        return null;
    }
    
    /**
     * Configures the client as needed.
     */
    public void configureClient(DefaultHttpClient client) {
    }
    
    /**
     * Builds a header array from the specified map.
     */
    public static Header[] getHeader(Map<String, String> headers) {
        Header[] headerArray = new Header[headers.size()];
        int index = 0;
        
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headerArray[index++] = new BasicHeader(entry.getKey(), entry.getValue());
        }
        
        return headerArray;
    }
    
    /**
     * Converts the specified value to a JSON-encoded {@link HttpEntity}.
     */
    public static HttpEntity getJsonHttpEntity(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            new ObjectMapper().writeValue(out, value);
        } catch (Exception exception) {
            fail(exception.getLocalizedMessage());
        }
        
        return new ByteArrayEntity(out.toByteArray(), ContentType.APPLICATION_JSON);
    }
    
}
