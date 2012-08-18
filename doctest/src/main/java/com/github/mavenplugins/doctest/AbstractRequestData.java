package com.github.mavenplugins.doctest;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
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
    
    public static HttpEntity getJsonHttpEntity(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            new ObjectMapper().writeValue(out, value);
        } catch (Exception exception) {
            fail(exception.getLocalizedMessage());
        }
        
        return new ByteArrayEntity(out.toByteArray(), ContentType.APPLICATION_JSON);
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
    
}
