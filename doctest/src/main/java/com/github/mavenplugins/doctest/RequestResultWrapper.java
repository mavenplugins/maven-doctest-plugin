package com.github.mavenplugins.doctest;

/**
 * A wrapper for the request, which stores the states for the later report plugin.
 */
public class RequestResultWrapper {
    
    protected String path;
    protected String requestLine;
    protected String[] header;
    protected String[] paremeters;
    protected String entity;
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getRequestLine() {
        return requestLine;
    }
    
    public void setRequestLine(String requestLine) {
        this.requestLine = requestLine;
    }
    
    public String[] getHeader() {
        return header;
    }
    
    public void setHeader(String[] header) {
        this.header = header;
    }
    
    public String[] getParemeters() {
        return paremeters;
    }
    
    public void setParemeters(String[] paremeters) {
        this.paremeters = paremeters;
    }
    
    public String getEntity() {
        return entity;
    }
    
    public void setEntity(String entity) {
        this.entity = entity;
    }
    
}