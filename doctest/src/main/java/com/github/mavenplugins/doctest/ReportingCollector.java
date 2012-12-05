package com.github.mavenplugins.doctest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsNames;
import org.junit.runners.model.FrameworkMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mavenplugins.doctest.formatter.EntityFormatter;

public class ReportingCollector {
    
    /**
     * An empty array for copying purpose.
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[] {};
    /**
     * The java backstore variable for the results of the doctests.
     */
    public static final String RESULT_PATH = "doctest.result.path";
    
    /**
     * The java back-store.
     */
    protected Preferences prefs = Preferences.userNodeForPackage(DoctestRunner.class);
    /**
     * the directory for the test results.
     */
    protected File path = new File(prefs.get(RESULT_PATH, "./target/doctests/"));
    /**
     * The doctest class.
     */
    protected Class<?> testClass;
    /**
     * Util for instancing classes.
     */
    protected ReflectionUtil reflectionUtil = new ReflectionUtil();
    /**
     * The json mapper.
     */
    protected ObjectMapper jsonMapper = new ObjectMapper();
    
    /**
     * Initiates the util with the specified test class.
     */
    public void init(Class<?> testClass) {
        this.testClass = testClass;
        reflectionUtil.init(testClass);
        
        if (!path.exists()) {
            path.mkdirs();
        }
    }
    
    /**
     * Saves the request data for later reporting.
     */
    protected void saveRequest(FrameworkMethod method, Object test, RequestData requestClass, HttpRequest request,
            byte[] requestData, RequestResultWrapper wrapper) throws Exception {
        File file = new File(path, getRequestResultFileName(method, requestClass));
        PrintStream stream = null;
        
        try {
            stream = new PrintStream(file);
            if (request instanceof HttpEntityEnclosingRequestBase && requestData != null) {
                setRequestEntity(test, (HttpEntityEnclosingRequestBase) request, requestData, wrapper,
                        getFormatter(method));
            }
            jsonMapper.writeValue(stream, wrapper);
        } finally {
            stream.flush();
            stream.close();
        }
    }
    
    /**
     * Gets the filename for the result file using the specified method.
     */
    protected String getRequestResultFileName(FrameworkMethod method, RequestData requestClass) {
        return testClass.getName() + "-" + method.getName() + "-" + requestClass.getClass().getSimpleName()
                + ".request";
    }
    
    /**
     * Assigns the request form data to the wrapper.
     */
    protected void setRequestEntity(Object test, HttpEntityEnclosingRequestBase request, byte[] requestData,
            RequestResultWrapper wrapper, Class<? extends EntityFormatter> formatterType)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            Exception, IOException {
        EntityFormatter formatter;
        
        if ((formatter = reflectionUtil.instance(test, formatterType)) != null) {
            wrapper.setEntity(formatter.format(request.getEntity(), requestData));
        } else {
            wrapper.setEntity(new String(requestData));
        }
    }
    
    /**
     * Assigns the request parameter to the wrapper.
     */
    protected void setRequestParameters(HttpRequest request, RequestResultWrapper wrapper) {
        HttpParams parameters = request.getParams();
        List<String> parameterValues = new ArrayList<String>();
        
        if (parameters instanceof HttpParamsNames) {
            try {
                for (String name : ((HttpParamsNames) parameters).getNames()) {
                    parameterValues.add(name + ": " + parameters.getParameter(name));
                }
            } catch (UnsupportedOperationException exception) {
            }
        }
        
        wrapper.setParemeters(parameterValues.toArray(EMPTY_STRING_ARRAY));
    }
    
    /**
     * Assigns the request headers to the wrapper.
     */
    public void setRequestHeaders(HttpRequest request, RequestResultWrapper wrapper) {
        String[] headerValues = new String[request.getAllHeaders().length];
        int index = 0;
        
        for (Header headers : request.getAllHeaders()) {
            headerValues[index++] = headers.getName() + ": " + headers.getValue();
        }
        
        wrapper.setHeader(headerValues);
    }
    
    /**
     * Assigns the request-line to the wrapper.
     */
    public void setRequestLine(HttpRequest request, RequestResultWrapper wrapper) throws URISyntaxException {
        StringBuilder builder = new StringBuilder();
        
        builder.append(request.getRequestLine().getProtocolVersion().toString());
        builder.append(' ');
        builder.append(request.getRequestLine().getMethod());
        builder.append(' ');
        builder.append(request.getRequestLine().getUri());
        
        wrapper.setRequestLine(builder.toString());
        wrapper.setPath(new URI(request.getRequestLine().getUri()).getRawPath());
    }
    
    /**
     * Saves the response data for reporting.
     */
    protected void saveResponse(FrameworkMethod method, Object test, RequestData requestClass, ResponseContext ctx)
            throws Exception {
        File file = new File(path, getResponseResultFileName(method, requestClass));
        PrintStream stream = null;
        ResponseResultWrapper wrapper;
        
        try {
            stream = new PrintStream(file);
            
            wrapper = new ResponseResultWrapper();
            if (ctx != null) {
                setResponseLine(ctx, wrapper);
                setResponseHeaders(ctx, wrapper);
                setResponseParameters(ctx, wrapper);
                setResponseEntity(method, test, ctx, wrapper, getFormatter(method));
            } else {
                // TODO: report no content
            }
            
            jsonMapper.writeValue(stream, wrapper);
        } finally {
            stream.flush();
            stream.close();
        }
    }
    
    private Class<? extends EntityFormatter> getFormatter(FrameworkMethod method) {
        Doctest doctest = method.getMethod().getAnnotation(Doctest.class);
        SimpleDoctest simpleDoctest = method.getMethod().getAnnotation(SimpleDoctest.class);
        
        return doctest == null ? simpleDoctest.formatter() : doctest.formatter();
    }
    
    /**
     * Gets the filename for the response result file.
     */
    protected String getResponseResultFileName(FrameworkMethod method, RequestData requestClass) {
        return testClass.getName() + "-" + method.getName() + "-" + requestClass.getClass().getSimpleName()
                + ".response";
    }
    
    /**
     * Assigns the response body to the wrapper.
     */
    protected void setResponseEntity(FrameworkMethod method, Object test, ResponseContext ctx,
            ResponseResultWrapper wrapper, Class<? extends EntityFormatter> formatterType)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            Exception, IOException {
        EntityFormatter formatter;
        
        if (ctx.getResponseData() != null) {
            if ((formatter = reflectionUtil.instance(test, formatterType)) != null) {
                wrapper.setEntity(formatter.format(ctx.getResponse().getEntity(), ctx.getResponseData()));
            } else {
                wrapper.setEntity(new String(ctx.getResponseData()));
            }
        }
    }
    
    /**
     * Assigns the response parameters to the wrapper.
     */
    protected void setResponseParameters(ResponseContext ctx, ResponseResultWrapper wrapper) {
        HttpParams parameters = ctx.getResponse().getParams();
        List<String> parameterValues = new ArrayList<String>();
        
        if (parameters instanceof HttpParamsNames) {
            try {
                for (String name : ((HttpParamsNames) parameters).getNames()) {
                    parameterValues.add(name + ": " + parameters.getParameter(name));
                }
            } catch (UnsupportedOperationException exception) {
            }
        }
        
        wrapper.setParemeters(parameterValues.toArray(EMPTY_STRING_ARRAY));
    }
    
    /**
     * Assigns the response header to the wrapper.
     */
    public void setResponseHeaders(ResponseContext ctx, ResponseResultWrapper wrapper) {
        String[] headerValues = new String[ctx.getResponse().getAllHeaders().length];
        int index = 0;
        
        for (Header headers : ctx.getResponse().getAllHeaders()) {
            headerValues[index++] = headers.getName() + ": " + headers.getValue();
        }
        
        wrapper.setHeader(headerValues);
    }
    
    /**
     * Assigns the response-line to the wrapper.
     */
    protected void setResponseLine(ResponseContext ctx, ResponseResultWrapper wrapper) {
        StringBuilder builder = new StringBuilder();
        HttpResponse response = ctx.getResponse();
        
        builder.append(response.getStatusLine().getProtocolVersion().toString());
        builder.append(' ');
        builder.append(response.getStatusLine().getStatusCode());
        builder.append(' ');
        builder.append(response.getStatusLine().getReasonPhrase());
        
        wrapper.setStatusLine(builder.toString());
    }
    
}
