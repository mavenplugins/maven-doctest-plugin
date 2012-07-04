package com.github.mavenplugins.doctest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpParams;

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
    
}
