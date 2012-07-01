package com.github.mavenplugins.doctest;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.List;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
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
import org.apache.http.protocol.HttpContext;
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

public class DoctestRunner extends BlockJUnit4ClassRunner {
    
    public static final String TEST_SOURCE_PATH = "doctest.sources.path";
    public static final String RESULT_PATH = "doctest.result.path";
    private static final RequestAcceptEncoding REQUEST_GZIP_INTERCEPTOR = new RequestAcceptEncoding();
    private static final ResponseContentEncoding RESPONSE_GZIP_INTERCEPTOR = new ResponseContentEncoding();
    
    protected Preferences prefs = Preferences.userNodeForPackage(DoctestRunner.class);
    protected File path = new File(prefs.get(RESULT_PATH, "./target/doctests/"));
    protected ObjectMapper jsonMapper = new ObjectMapper();
    
    public DoctestRunner(Class<?> testClass) throws InvocationTargetException, InitializationError {
        super(testClass);
        if (!path.exists()) {
            path.mkdirs();
        }
    }
    
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        return getTestClass().getAnnotatedMethods(Doctest.class);
    }
    
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
                client.addRequestInterceptor(new HttpRequestInterceptor() {
                    
                    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                        saveRequest(method, request);
                    }
                    
                });
                
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
                saveResponse(method, test, response, responseData);
                
                assertExpectations(method, response);
                
                invokeTestMethod(method, test, response, methodParameters, responseData);
            }
            
        };
    }
    
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
    
    protected void assertResponseHeaders(HttpResponse response, ExpectHeaders headers) {
        ExpectHeader[] headerArray = headers.value();
        
        if (headerArray != null && headerArray.length > 0) {
            for (ExpectHeader header : headerArray) {
                HttpResponseAssertUtils.assertHeaderContains(response, header.name(), header.content());
            }
        }
    }
    
    protected void assertResponseStatus(HttpResponse response, ExpectStatus status) {
        assertEquals(status.value(), response.getStatusLine().getStatusCode());
        if (!status.message().equals("")) {
            assertEquals(status.message(), response.getStatusLine().getReasonPhrase());
        }
    }
    
    protected void saveRequest(FrameworkMethod method, HttpRequest request) throws IOException {
        File file = new File(path, getRequestResultFileName(method));
        PrintStream stream = null;
        
        try {
            stream = new PrintStream(file);
            
            printRequestLine(request, stream);
            stream.println();
            printRequestHeaders(request, stream);
            stream.println();
            printRequestParameters(request, stream);
            if (request instanceof HttpEntityEnclosingRequest) {
                stream.println();
                printRequestEntity(request, stream);
            }
        } finally {
            stream.flush();
            stream.close();
        }
    }
    
    protected String getRequestResultFileName(FrameworkMethod method) {
        return getTestClass().getJavaClass().getName() + "-" + method.getName() + ".request";
    }
    
    protected void printRequestEntity(HttpRequest request, PrintStream stream) throws IOException {
        ((HttpEntityEnclosingRequest) request).getEntity().writeTo(stream);
    }
    
    protected void printRequestParameters(HttpRequest request, PrintStream stream) {
        HttpParams parameters = request.getParams();
        
        if (parameters instanceof HttpParamsNames) {
            try {
                for (String name : ((HttpParamsNames) parameters).getNames()) {
                    stream.print(name);
                    stream.print(": ");
                    stream.println(parameters.getParameter(name));
                }
            } catch (UnsupportedOperationException exception) {
            }
        }
    }
    
    protected void printRequestHeaders(HttpRequest request, PrintStream stream) {
        for (Header headers : request.getAllHeaders()) {
            stream.print(headers.getName());
            stream.print(": ");
            stream.println(headers.getValue());
        }
    }
    
    protected void printRequestLine(HttpRequest request, PrintStream stream) {
        stream.print(request.getRequestLine().getProtocolVersion().toString());
        stream.print(' ');
        stream.print(request.getRequestLine().getMethod());
        stream.print(' ');
        stream.println(request.getRequestLine().getUri());
    }
    
    protected void saveResponse(FrameworkMethod method, Object test, HttpResponse response, byte[] responseData) throws Exception {
        File file = new File(path, getResponseResultFileName(method));
        PrintStream stream = null;
        Doctest doctest = method.getMethod().getAnnotation(Doctest.class);
        
        try {
            stream = new PrintStream(file);
            
            printResponseLine(response, stream);
            stream.println();
            printResponseHeaders(response, stream);
            stream.println();
            printResponseParameters(response, stream);
            stream.println();
            printResponseEntity(method, test, response, responseData, stream, doctest.formatter());
        } finally {
            stream.flush();
            stream.close();
        }
    }
    
    protected String getResponseResultFileName(FrameworkMethod method) {
        return getTestClass().getJavaClass().getName() + "-" + method.getName() + ".response";
    }
    
    protected void printResponseEntity(FrameworkMethod method, Object test, HttpResponse response, byte[] responseData, PrintStream stream,
            Class<? extends EntityFormatter> formmterType) throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, Exception, IOException {
        EntityFormatter formatter;
        
        if ((formatter = instance(method, test, formmterType)) != null) {
            stream.print(formatter.format(response.getEntity(), responseData));
        } else {
            stream.write(responseData);
        }
    }
    
    protected void printResponseParameters(HttpResponse response, PrintStream stream) {
        HttpParams parameters;
        parameters = response.getParams();
        if (parameters instanceof HttpParamsNames) {
            try {
                for (String name : ((HttpParamsNames) parameters).getNames()) {
                    stream.print(name);
                    stream.print(": ");
                    stream.println(parameters.getParameter(name));
                }
            } catch (UnsupportedOperationException exception) {
            }
        }
    }
    
    protected void printResponseHeaders(HttpResponse response, PrintStream stream) {
        for (Header headers : response.getAllHeaders()) {
            stream.print(headers.getName());
            stream.print(": ");
            stream.println(headers.getValue());
        }
    }
    
    protected void printResponseLine(HttpResponse response, PrintStream stream) {
        stream.print(response.getStatusLine().getProtocolVersion().toString());
        stream.print(' ');
        stream.print(response.getStatusLine().getStatusCode());
        stream.print(' ');
        stream.println(response.getStatusLine().getReasonPhrase());
    }
    
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
    
    protected void invokeTestMethod(final FrameworkMethod method, final Object test, HttpResponse response, Class<?>[] methodParameters, byte[] responseData)
            throws Throwable, SAXException, IOException, ParserConfigurationException, JsonProcessingException {
        if (methodParameters.length == 1) {
            method.invokeExplosively(test, response);
        } else if (methodParameters.length == 2) {
            if (Document.class.isAssignableFrom(methodParameters[1])) {
                method.invokeExplosively(test, response, DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(responseData)));
            } else if (JsonNode.class.isAssignableFrom(methodParameters[1]) && response.getEntity() != null) {
                method.invokeExplosively(test, response, jsonMapper.readTree(responseData));
            }
        }
    }
    
}
