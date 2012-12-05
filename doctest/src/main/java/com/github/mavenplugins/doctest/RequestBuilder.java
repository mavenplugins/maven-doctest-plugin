package com.github.mavenplugins.doctest;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.message.BasicHeader;
import org.junit.runners.model.FrameworkMethod;

import de.odysseus.el.util.SimpleContext;

/**
 * HelperClass for building request objects.
 */
public class RequestBuilder {
    
    /**
     * The default test uri.
     */
    protected URI defaultURI;
    /**
     * Util for instancing classes.
     */
    protected ReflectionUtil reflectionUtil = new ReflectionUtil();
    /**
     * The factory for expressions.
     */
    protected ExpressionFactory factory;
    
    /**
     * Initiates the default URI.
     */
    public void init(Class<?> testClass) throws URISyntaxException {
        defaultURI = getURI(testClass);
        reflectionUtil.init(testClass);
        
        factory = ExpressionFactory.newInstance();
    }
    
    /**
     * Gets the uri for a doctest from the class or test method.
     */
    protected URI getURI(AnnotatedElement element) throws URISyntaxException {
        DoctestHost doctestHost;
        
        if (element.isAnnotationPresent(DoctestHost.class)) {
            doctestHost = element.getAnnotation(DoctestHost.class);
            return new URI(doctestHost.scheme(), null, doctestHost.host(), doctestHost.port(),
                    doctestHost.contextPath(), null, null);
        }
        
        return null;
    }
    
    /**
     * Builds the request based on the specified data..
     */
    public HttpRequestBase buildRequest(RequestData requestData, Method method, DoctestContext context)
            throws URISyntaxException {
        HttpRequestBase request = null;
        URI uri = requestData.getURI();
        URI methodURI = getURI(method);
        URI requestURI = null;
        ELContext expressionContext = new SimpleContext();
        ValueExpression expression;
        String path;
        String scheme;
        String userInfo;
        String host;
        String query;
        String fragment;
        int port;
        
        for (Map.Entry<String, Object> entry : context.getStore().entrySet()) {
            expression = factory.createValueExpression(expressionContext, "${" + entry.getKey() + "}", entry.getValue()
                    .getClass());
            expression.setValue(expressionContext, entry.getValue());
        }
        
        if (uri == null) {
            path = getPath(methodURI, defaultURI);
            
            if (path != null) {
                if (path.endsWith("/") && requestData.getPath().startsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                path = path + requestData.getPath();
            } else {
                path = requestData.getPath();
            }
            
            scheme = getScheme(methodURI, defaultURI);
            scheme = evalValue(scheme, expressionContext);
            userInfo = getUserInfo(methodURI, defaultURI);
            userInfo = evalValue(userInfo, expressionContext);
            host = getHost(methodURI, defaultURI);
            host = evalValue(host, expressionContext);
            query = getQuery(methodURI, defaultURI);
            query = evalValue(query, expressionContext);
            fragment = getFragment(methodURI, defaultURI);
            fragment = evalValue(fragment, expressionContext);
            path = evalValue(path, expressionContext);
            port = getPort(methodURI, defaultURI);
            
            requestURI = new URI(scheme, userInfo, host, port, path, query, fragment);
            
            uri = requestURI;
        }
        
        if (HttpGet.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpGet(uri);
        } else if (HttpPost.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpPost(uri);
        } else if (HttpPut.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpPut(uri);
        } else if (HttpOptions.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpOptions(uri);
        } else if (HttpHead.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpHead(uri);
        } else if (HttpDelete.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpDelete(uri);
        } else if (HttpPatch.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpPatch(uri);
        } else if (HttpTrace.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpTrace(uri);
        }
        
        return request;
    }
    
    protected String evalValue(String pattern, ELContext expressionContext) {
        ValueExpression expression;
        
        if (pattern == null) {
            return null;
        } else {
            expression = factory.createValueExpression(expressionContext, pattern, String.class);
            return (String) expression.getValue(expressionContext);
        }
    }
    
    protected String getScheme(URI uri1, URI uri2) {
        return uri1 != null && uri1.getScheme() != null ? uri1.getScheme()
                : (uri2 != null && uri2.getScheme() != null ? uri2.getScheme() : null);
    }
    
    protected int getPort(URI uri1, URI uri2) {
        return uri1 != null && uri1.getPort() != -1 ? uri1.getPort() : (uri2 != null && uri2.getPort() != -1 ? uri2
                .getPort() : 80);
    }
    
    protected String getUserInfo(URI uri1, URI uri2) {
        return uri1 != null && uri1.getUserInfo() != null ? uri1.getUserInfo() : (uri2 != null
                && uri2.getUserInfo() != null ? uri2.getUserInfo() : null);
    }
    
    protected String getHost(URI uri1, URI uri2) {
        return uri1 != null && uri1.getHost() != null ? uri1.getHost() : (uri2 != null && uri2.getHost() != null ? uri2
                .getHost() : null);
    }
    
    protected String getPath(URI uri1, URI uri2) {
        return uri1 != null && uri1.getPath() != null ? uri1.getPath() : (uri2 != null && uri2.getPath() != null ? uri2
                .getPath() : null);
    }
    
    protected String getQuery(URI uri1, URI uri2) {
        return uri1 != null && uri1.getQuery() != null ? uri1.getQuery()
                : (uri2 != null && uri2.getQuery() != null ? uri2.getQuery() : null);
    }
    
    protected String getFragment(URI uri1, URI uri2) {
        return uri1 != null && uri1.getFragment() != null ? uri1.getFragment() : (uri2 != null
                && uri2.getFragment() != null ? uri2.getFragment() : null);
    }
    
    /**
     * Extracts the requestData from the doctest-method.
     */
    protected RequestData[] getRequestData(final FrameworkMethod method, final Object test, DoctestContext context)
            throws Exception {
        RequestData[] requestData;
        final SimpleDoctest doctest;
        Class<? extends RequestData>[] requestClasses;
        final String path;
        final URI uri;
        final ELContext expressionContext = new SimpleContext();
        ValueExpression expression;
        String tmp;
        
        if (method.getMethod().isAnnotationPresent(Doctest.class)) {
            requestClasses = method.getMethod().getAnnotation(Doctest.class).value();
            
            requestData = new RequestData[requestClasses.length];
            for (int i = 0; i < requestClasses.length; i++) {
                requestData[i] = reflectionUtil.instance(test, requestClasses[i]);
            }
        } else {
            doctest = method.getMethod().getAnnotation(SimpleDoctest.class);
            
            for (Map.Entry<String, Object> entry : context.getStore().entrySet()) {
                expression = factory.createValueExpression(expressionContext, "${" + entry.getKey() + "}", entry
                        .getValue().getClass());
                expression.setValue(expressionContext, entry.getValue());
            }
            
            tmp = doctest.value();
            tmp = evalValue(tmp, expressionContext);
            
            if (doctest.value().startsWith("/")) {
                path = tmp;
                uri = null;
            } else {
                path = null;
                uri = new URI(tmp);
            }
            
            requestData = new RequestData[] { new AbstractRequestData() {
                
                @Override
                public URI getURI() throws URISyntaxException {
                    return uri;
                }
                
                @Override
                public String getPath() {
                    return path;
                }
                
                @Override
                public String getMethod() {
                    return doctest.method();
                }
                
                @Override
                public Header[] getHeaders() {
                    Header[] headers = new Header[doctest.header().length];
                    int index = 0, i;
                    String name, value;
                    
                    for (String header : doctest.header()) {
                        i = header.indexOf(':');
                        
                        name = evalValue(header.substring(0, i).trim(), expressionContext);
                        value = evalValue(header.substring(i + 1).trim(), expressionContext);
                        headers[index++] = new BasicHeader(name, value);
                    }
                    
                    return headers;
                };
                
            } };
        }
        
        return requestData;
    }
    
}
