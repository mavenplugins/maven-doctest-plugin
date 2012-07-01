package com.github.mavenplugins.doctest;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.Credentials;
import org.apache.http.params.HttpParams;

public interface RequestData {
    
    URI getURI() throws URISyntaxException;
    
    String getMethod();
    
    HttpParams getParameters();
    
    Header[] getHeaders();
    
    HttpEntity getHttpEntity();
    
    Credentials getCredentials();
    
}
