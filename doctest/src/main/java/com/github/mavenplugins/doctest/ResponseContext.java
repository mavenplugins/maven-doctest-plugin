package com.github.mavenplugins.doctest;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * ThreadContext for responses.
 */
public class ResponseContext {
    
    /**
     * The http response;
     */
    HttpResponse response = null;
    /**
     * The response payload.
     */
    byte[] responseData = null;
    /**
     * The response payload as object.
     */
    Object entity = null;
    /**
     * The client with which the request was executed.
     */
    HttpClient httpClient;
    /**
     * The context object for the request.
     */
    HttpContext httpContext;
    /**
     * The cookie store for the request.
     */
    CookieStore cookieStore;
    
    public CookieStore getCookieStore() {
        return cookieStore;
    }
    
    public void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }
    
    public HttpContext getHttpContext() {
        return httpContext;
    }
    
    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }
    
    public HttpClient getHttpClient() {
        return httpClient;
    }
    
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    public Object getEntity() {
        return entity;
    }
    
    public void setEntity(Object entity) {
        this.entity = entity;
    }
    
    public HttpResponse getResponse() {
        return response;
    }
    
    public void setResponse(HttpResponse response) {
        this.response = response;
    }
    
    public byte[] getResponseData() {
        return responseData;
    }
    
    public void setResponseData(byte[] responseData) {
        this.responseData = responseData;
    }
    
}