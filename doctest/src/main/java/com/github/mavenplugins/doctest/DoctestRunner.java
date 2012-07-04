package com.github.mavenplugins.doctest;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsNames;
import org.apache.http.util.EntityUtils;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mavenplugins.doctest.asserts.HttpResponseAssertUtils;
import com.github.mavenplugins.doctest.expectations.ExpectHeader;
import com.github.mavenplugins.doctest.expectations.ExpectHeaders;
import com.github.mavenplugins.doctest.expectations.ExpectStatus;
import com.github.mavenplugins.doctest.formatter.EntityFormatter;

/**
 * A TestRunner only for doctests.
 */
public class DoctestRunner extends BlockJUnit4ClassRunner {
    
    /**
     * An empty array for copying purpose.
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[] {};
    /**
     * The java backstore variable for the sources of the doctests.
     */
    public static final String TEST_SOURCE_PATH = "doctest.sources.path";
    /**
     * The java backstore variable for the results of the doctests.
     */
    public static final String RESULT_PATH = "doctest.result.path";
    /**
     * Enable the gzip.
     */
    private static final RequestAcceptEncoding REQUEST_GZIP_INTERCEPTOR = new RequestAcceptEncoding();
    /**
     * Enable the gzip.
     */
    private static final ResponseContentEncoding RESPONSE_GZIP_INTERCEPTOR = new ResponseContentEncoding();
    
    /**
     * The java back-store.
     */
    protected Preferences prefs = Preferences.userNodeForPackage(DoctestRunner.class);
    /**
     * the directory for the test results.
     */
    protected File path = new File(prefs.get(RESULT_PATH, "./target/doctests/"));
    /**
     * The json mapper.
     */
    protected ObjectMapper jsonMapper = new ObjectMapper();
    
    /**
     * constructs the runner with the given test class.
     */
    public DoctestRunner(Class<?> testClass) throws InvocationTargetException, InitializationError {
        super(testClass);
        if (!path.exists()) {
            path.mkdirs();
        }
    }
    
    /**
     * Gets only methods annotated with {@link Doctest}.
     */
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        return getTestClass().getAnnotatedMethods(Doctest.class);
    }
    
    /**
     * Gets the executor for the doctests.
     */
    @Override
    protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return new Statement() {
            
            @Override
            public void evaluate() throws Throwable {
                HttpResponse response = null;
                HttpRequestBase request = null;
                RequestData requestData = instance(method, test, method.getMethod().getAnnotation(Doctest.class).value());
                DefaultHttpClient client = new DefaultHttpClient();
                BasicCredentialsProvider credentialsProvider;
                Class<?>[] methodParameters = method.getMethod().getParameterTypes();
                byte[] responseData;
                
                client.addRequestInterceptor(REQUEST_GZIP_INTERCEPTOR);
                client.addResponseInterceptor(RESPONSE_GZIP_INTERCEPTOR);
                
                request = buildRequest(request, requestData);
                
                if (requestData.getHeaders() != null) {
                    request.setHeaders(requestData.getHeaders());
                }
                
                if (requestData.getParameters() != null) {
                    request.setParams(requestData.getParameters());
                }
                
                if (requestData.getHttpEntity() != null && request instanceof HttpEntityEnclosingRequest) {
                    ((HttpEntityEnclosingRequest) request).setEntity(requestData.getHttpEntity());
                }
                
                if (requestData.getCredentials() != null) {
                    credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY, requestData.getCredentials());
                    client.setCredentialsProvider(credentialsProvider);
                }
                
                response = client.execute(request);
                responseData = EntityUtils.toByteArray(response.getEntity());
                saveRequest(method, request);
                saveResponse(method, test, response, responseData);
                
                assertExpectations(method, response);
                
                invokeTestMethod(method, test, response, methodParameters, responseData);
            }
            
        };
    }
    
    /**
     * Verifies the expect-annotations, if any, of the method.
     */
    protected void assertExpectations(FrameworkMethod method, HttpResponse response) {
        ExpectStatus status = method.getMethod().getAnnotation(ExpectStatus.class);
        ExpectHeaders headers = method.getMethod().getAnnotation(ExpectHeaders.class);
        
        if (status != null) {
            assertResponseStatus(response, status);
        }
        
        if (headers != null) {
            assertResponseHeaders(response, headers);
        }
    }
    
    /**
     * Verifies if the expected headers were given.
     */
    protected void assertResponseHeaders(HttpResponse response, ExpectHeaders headers) {
        ExpectHeader[] headerArray = headers.value();
        
        if (headerArray != null && headerArray.length > 0) {
            for (ExpectHeader header : headerArray) {
                HttpResponseAssertUtils.assertHeaderContains(response, header.name(), header.content());
            }
        }
    }
    
    /**
     * Verifies if the expected status code was given.
     */
    protected void assertResponseStatus(HttpResponse response, ExpectStatus status) {
        assertEquals(status.value(), response.getStatusLine().getStatusCode());
        if (!status.message().equals("")) {
            assertEquals(status.message(), response.getStatusLine().getReasonPhrase());
        }
    }
    
    /**
     * Saves the request data for later reporting.
     */
    protected void saveRequest(FrameworkMethod method, HttpRequest request) throws Exception {
        File file = new File(path, getRequestResultFileName(method));
        PrintStream stream = null;
        RequestResultWrapper wrapper;
        
        try {
            stream = new PrintStream(file);
            
            wrapper = new RequestResultWrapper();
            setRequestLine(request, wrapper);
            setRequestHeaders(request, wrapper);
            setRequestParameters(request, wrapper);
            if (request instanceof HttpEntityEnclosingRequest) {
                setRequestEntity(request, wrapper);
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
    protected String getRequestResultFileName(FrameworkMethod method) {
        return getTestClass().getJavaClass().getName() + "-" + method.getName() + ".request";
    }
    
    /**
     * Assigns the request form data to the wrapper.
     */
    protected void setRequestEntity(HttpRequest request, RequestResultWrapper wrapper) throws IOException {
        if (((HttpEntityEnclosingRequest) request).getEntity() != null) {
            wrapper.setEntity(EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity()));
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
    protected void setRequestHeaders(HttpRequest request, RequestResultWrapper wrapper) {
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
    protected void setRequestLine(HttpRequest request, RequestResultWrapper wrapper) throws URISyntaxException {
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
     * Saves the response dta for later reporting.
     */
    protected void saveResponse(FrameworkMethod method, Object test, HttpResponse response, byte[] responseData) throws Exception {
        File file = new File(path, getResponseResultFileName(method));
        PrintStream stream = null;
        Doctest doctest = method.getMethod().getAnnotation(Doctest.class);
        ResponseResultWrapper wrapper;
        
        try {
            stream = new PrintStream(file);
            
            wrapper = new ResponseResultWrapper();
            setResponseLine(response, wrapper);
            setResponseHeaders(response, wrapper);
            setResponseParameters(response, wrapper);
            setResponseEntity(method, test, response, responseData, wrapper, doctest.formatter());
            
            jsonMapper.writeValue(stream, wrapper);
        } finally {
            stream.flush();
            stream.close();
        }
    }
    
    /**
     * Gets the filename for the response result file.
     */
    protected String getResponseResultFileName(FrameworkMethod method) {
        return getTestClass().getJavaClass().getName() + "-" + method.getName() + ".response";
    }
    
    /**
     * Assigns the response body to the wrapper.
     */
    protected void setResponseEntity(FrameworkMethod method, Object test, HttpResponse response, byte[] responseData, ResponseResultWrapper wrapper,
            Class<? extends EntityFormatter> formmterType) throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, Exception, IOException {
        EntityFormatter formatter;
        
        if ((formatter = instance(method, test, formmterType)) != null) {
            wrapper.setEntity(formatter.format(response.getEntity(), responseData));
        } else {
            wrapper.setEntity(new String(responseData));
        }
    }
    
    /**
     * Assigns the response parameters to the wrapper.
     */
    protected void setResponseParameters(HttpResponse response, ResponseResultWrapper wrapper) {
        HttpParams parameters = response.getParams();
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
    protected void setResponseHeaders(HttpResponse response, ResponseResultWrapper wrapper) {
        String[] headerValues = new String[response.getAllHeaders().length];
        int index = 0;
        
        for (Header headers : response.getAllHeaders()) {
            headerValues[index++] = headers.getName() + ": " + headers.getValue();
        }
        
        wrapper.setHeader(headerValues);
    }
    
    /**
     * Assigns the response-line to the wrapper.
     */
    protected void setResponseLine(HttpResponse response, ResponseResultWrapper wrapper) {
        StringBuilder builder = new StringBuilder();
        
        builder.append(response.getStatusLine().getProtocolVersion().toString());
        builder.append(' ');
        builder.append(response.getStatusLine().getStatusCode());
        builder.append(' ');
        builder.append(response.getStatusLine().getReasonPhrase());
        
        wrapper.setStatusLine(builder.toString());
    }
    
    /**
     * Instantiates the given type.
     */
    protected <T> T instance(FrameworkMethod method, Object test, Class<T> type) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException {
        Constructor<T> constructor;
        
        if (type == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        
        if (type.getEnclosingClass() != null && type.getEnclosingClass().equals(getTestClass().getJavaClass()) && !Modifier.isStatic(type.getModifiers())) {
            constructor = type.getDeclaredConstructor(getTestClass().getJavaClass());
            
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            
            return constructor.newInstance(test);
        } else {
            constructor = type.getDeclaredConstructor();
            
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            
            return constructor.newInstance();
        }
    }
    
    /**
     * Checks if the test method suits the needs of the test-runner.
     */
    @Override
    protected void validateTestMethods(List<Throwable> errors) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Doctest.class);
        Class<?>[] parameters;
        boolean valid;
        
        for (FrameworkMethod eachTestMethod : methods) {
            eachTestMethod.validatePublicVoid(false, errors);
            parameters = eachTestMethod.getMethod().getParameterTypes();
            
            valid = parameters.length == 1 && HttpResponse.class.isAssignableFrom(parameters[0]);
            if (!valid) {
                valid = parameters.length == 2 && HttpResponse.class.isAssignableFrom(parameters[0])
                        && (JsonNode.class.isAssignableFrom(parameters[1]) || Document.class.isAssignableFrom(parameters[1]));
            }
            
            if (!valid) {
                errors.add(new IllegalArgumentException(
                        "doctest methods needs to have the first parameter of type org.apache.http.HttpResponse and optionally the second as com.fasterxml.jackson.databind.JsonNode or org.w3c.dom.Document."));
            }
        }
    }
    
    /**
     * Builds the request based on the specified data..
     */
    protected HttpRequestBase buildRequest(HttpRequestBase request, RequestData requestData) throws URISyntaxException {
        if (HttpGet.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpGet(requestData.getURI());
        } else if (HttpPost.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpPost(requestData.getURI());
        } else if (HttpPut.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpPut(requestData.getURI());
        } else if (HttpOptions.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpOptions(requestData.getURI());
        } else if (HttpHead.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpHead(requestData.getURI());
        } else if (HttpDelete.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpDelete(requestData.getURI());
        } else if (HttpPatch.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpPatch(requestData.getURI());
        } else if (HttpTrace.METHOD_NAME.equalsIgnoreCase(requestData.getMethod())) {
            request = new HttpTrace(requestData.getURI());
        }
        return request;
    }
    
    /**
     * Actually runs the test method.
     */
    protected void invokeTestMethod(final FrameworkMethod method, final Object test, HttpResponse response, Class<?>[] methodParameters, byte[] responseData)
            throws Throwable, SAXException, IOException, ParserConfigurationException, JsonProcessingException {
        if (methodParameters.length == 1) {
            method.invokeExplosively(test, response);
        } else if (methodParameters.length == 2) {
            if (Document.class.isAssignableFrom(methodParameters[1])) {
                if (responseData == null || responseData.length == 0) {
                    method.invokeExplosively(test, response, null);
                } else {
                    method.invokeExplosively(test, response,
                            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(responseData)));
                }
            } else if (JsonNode.class.isAssignableFrom(methodParameters[1]) && response.getEntity() != null) {
                if (responseData == null || responseData.length == 0) {
                    method.invokeExplosively(test, response, null);
                } else {
                    method.invokeExplosively(test, response, jsonMapper.readTree(responseData));
                }
            }
        }
    }
    
}