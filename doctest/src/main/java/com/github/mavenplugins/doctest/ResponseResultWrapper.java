package com.github.mavenplugins.doctest;

public class ResponseResultWrapper {
    
    protected String statusLine;
    protected String[] header;
    protected String[] paremeters;
    protected String entity;
    
    public String getStatusLine() {
        return statusLine;
    }
    
    public void setStatusLine(String responseLine) {
        this.statusLine = responseLine;
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