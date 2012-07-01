package com.github.mavenplugins.doctest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpParams;

public abstract class AbstractRequestData implements RequestData {
    
    public HttpParams getParameters() {
        return null;
    }
    
    public String getMethod() {
        return HttpGet.METHOD_NAME;
    }
    
    public Header[] getHeaders() {
        return null;
    }
    
    public HttpEntity getHttpEntity() {
        return null;
    }
    
    public Credentials getCredentials() {
        return null;
    }
    
}
