package com.github.mavenplugins.doctest;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

/**
 * Represents the configuration for a doctest request.
 */
public interface RequestData {
    
    /**
     * Gets the uri for a doctest.
     */
    URI getURI() throws URISyntaxException;
    
    /**
     * Gets the string representation for a http method.
     */
    String getMethod();
    
    /**
     * Gets the request parameters.
     */
    HttpParams getParameters();
    
    /**
     * Gets the request headers.
     */
    Header[] getHeaders();
    
    /**
     * Gets the entity (form data) for post or put requests.
     */
    HttpEntity getHttpEntity();
    
    /**
     * Gets the authentication credentials.
     */
    Credentials getCredentials();
    
    /**
     * Configures the client as needed.
     */
    void configureClient(DefaultHttpClient client);
    
}
