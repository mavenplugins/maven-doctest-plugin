package com.github.mavenplugins.doctest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsNames;
import org.apache.http.protocol.BasicHttpContext;
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
import com.github.mavenplugins.doctest.DoctestConfig.AssertionMode;
import com.github.mavenplugins.doctest.DoctestCookieConfig.Store;
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
     * ThreadContext for responses.
     */
    protected class ResponseContext {
        
        /**
         * The http response;
         */
        HttpResponse response = null;
        /**
         * The response payload.
         */
        byte[] responseData = null;
        
    }
    
    /**
     * Turns a response into an object.
     */
    protected interface ResponseDeserializer {
        
        /**
         * Returns the object represented through the data.
         */
        Object deserialize(byte[] data, Class<?> valueType) throws Exception;
        
    }
    
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
     * context object for asynchronous request execution.
     */
    protected ThreadLocal<ResponseContext> responses = new ThreadLocal<DoctestRunner.ResponseContext>() {
        
        @Override
        protected ResponseContext initialValue() {
            return new ResponseContext();
        }
        
    };
    /**
     * A mapping with content-types to {@link ResponseDeserializer} objects.
     */
    protected Map<String, ResponseDeserializer> responseDeserializers = new HashMap<String, ResponseDeserializer>();
    /**
     * A mapping with cookie stores.
     */
    protected Map<String, CookieStore> cookieStores = new HashMap<String, CookieStore>();
    /**
     * The standard store for cookies.
     */
    protected CookieStore defaultCookieStore;
    /**
     * The standard store strategy for cookies.
     */
    protected Store defaultCookieStoreStrategy = Store.SHARED;
    
    /**
     * constructs the runner with the given test class.
     */
    public DoctestRunner(Class<?> testClass) throws InvocationTargetException, InitializationError {
        super(testClass);
        if (!path.exists()) {
            path.mkdirs();
        }
        
        ResponseDeserializer jsonDeserializer = new ResponseDeserializer() {
            
            @Override
            public Object deserialize(byte[] data, Class<?> valueType) throws Exception {
                return jsonMapper.readValue(data, valueType);
            }
            
        };
        ResponseDeserializer xmlDeserializer = new ResponseDeserializer() {
            
            @Override
            public Object deserialize(byte[] data, Class<?> valueType) throws Exception {
                Unmarshaller unmarshaller = JAXBContext.newInstance(valueType).createUnmarshaller();
                return unmarshaller.unmarshal(new ByteArrayInputStream(data));
            }
            
        };
        
        responseDeserializers.put(ContentType.APPLICATION_JSON.getMimeType(), jsonDeserializer);
        responseDeserializers.put(ContentType.APPLICATION_XML.getMimeType(), xmlDeserializer);
        responseDeserializers.put(ContentType.APPLICATION_ATOM_XML.getMimeType(), xmlDeserializer);
        responseDeserializers.put(ContentType.APPLICATION_SVG_XML.getMimeType(), xmlDeserializer);
        responseDeserializers.put(ContentType.TEXT_XML.getMimeType(), xmlDeserializer);
        
        if (!testClass.isAnnotationPresent(DoctestCookieConfig.class)) {
            defaultCookieStore = new BasicCookieStore();
        } else {
            DoctestCookieConfig config = testClass.getAnnotation(DoctestCookieConfig.class);
            
            defaultCookieStoreStrategy = config.store();
            if (config.name() != null && !config.name().trim().isEmpty()) {
                if (config.store() == Store.NEW) {
                    cookieStores.put(config.name(), null);
                } else {
                    cookieStores.put(config.name(), new BasicCookieStore());
                }
            }
        }
    }
    
    /**
     * Gets only methods annotated with {@link Doctest} and {@link SimpleDoctest}.
     */
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> list = new ArrayList<FrameworkMethod>();
        
        list.addAll(getTestClass().getAnnotatedMethods(Doctest.class));
        list.addAll(getTestClass().getAnnotatedMethods(SimpleDoctest.class));
        
        if (cookieStores == null) {
            cookieStores = new HashMap<String, CookieStore>();
        }
        cookieStores.clear();
        
        for (FrameworkMethod method : list) {
            if (method.getMethod().isAnnotationPresent(DoctestCookieConfig.class)) {
                DoctestCookieConfig config = method.getMethod().getAnnotation(DoctestCookieConfig.class);
                if (config != null && config.name() != null && !config.name().trim().isEmpty()) {
                    if (config.store() == Store.NEW) {
                        cookieStores.put(config.name(), null);
                    } else {
                        cookieStores.put(config.name(), new BasicCookieStore());
                    }
                }
            }
        }
        
        Collections.sort(list, new Comparator<FrameworkMethod>() {
            
            @Override
            public int compare(FrameworkMethod o1, FrameworkMethod o2) {
                DoctestOrder order1 = o1.getMethod().getAnnotation(DoctestOrder.class);
                DoctestOrder order2 = o2.getMethod().getAnnotation(DoctestOrder.class);
                
                return compareDoctestOrder(order1, order2);
            }
            
        });
        
        return list;
    }
    
    /**
     * Compares the two doctestOrder instances representing doctests.
     */
    protected int compareDoctestOrder(DoctestOrder order1, DoctestOrder order2) {
        if (order1 != null && order2 != null) {
            return Integer.valueOf(order1.value()).compareTo(order2.value());
        } else if (order1 != null) {
            return Integer.valueOf(order1.value()).compareTo(0);
        } else if (order2 != null) {
            return Integer.valueOf(0).compareTo(order2.value());
        }
        
        return 0;
    }
    
    /**
     * Gets the executor for the doctests.
     */
    @Override
    protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return new Statement() {
            
            @Override
            public void evaluate() throws Throwable {
                final RequestData[] requestData;
                final DoctestClient clientConfig = method.getMethod().getAnnotation(DoctestClient.class);
                final DoctestConfig doctestConfig = method.getMethod().getAnnotation(DoctestConfig.class);
                
                requestData = getRequestData(method, test);
                
                for (RequestData data : requestData) {
                    executeTestcase(method, test, data, clientConfig, doctestConfig);
                }
            }
            
        };
    }
    
    /**
     * Verifies the expect-annotations, if any, of the method.
     */
    protected void assertExpectations(FrameworkMethod method, HttpResponse response, RequestData requestClass) {
        ExpectStatus status = method.getMethod().getAnnotation(ExpectStatus.class);
        ExpectHeaders headers = method.getMethod().getAnnotation(ExpectHeaders.class);
        
        if (status != null) {
            assertResponseStatus(response, status, requestClass);
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
    protected void assertResponseStatus(HttpResponse response, ExpectStatus status, RequestData requestClass) {
        assertEquals("Wrong Statuscode for request: " + requestClass.getClass().getSimpleName(), status.value(),
                response.getStatusLine().getStatusCode());
        if (!status.message().equals("")) {
            assertEquals("Wrong Status-line for request: " + requestClass.getClass().getSimpleName(), status.message(),
                    response.getStatusLine().getReasonPhrase());
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
                setRequestEntity(method, test, (HttpEntityEnclosingRequestBase) request, requestData, wrapper,
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
        return getTestClass().getJavaClass().getName() + "-" + method.getName() + "-"
                + requestClass.getClass().getSimpleName() + ".request";
    }
    
    /**
     * Assigns the request form data to the wrapper.
     */
    protected void setRequestEntity(FrameworkMethod method, Object test, HttpEntityEnclosingRequestBase request,
            byte[] requestData, RequestResultWrapper wrapper, Class<? extends EntityFormatter> formatterType)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            Exception, IOException {
        EntityFormatter formatter;
        
        if ((formatter = instance(method, test, formatterType)) != null) {
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
     * Saves the response data for reporting.
     */
    protected void saveResponse(FrameworkMethod method, Object test, RequestData requestClass, HttpResponse response,
            byte[] responseData) throws Exception {
        File file = new File(path, getResponseResultFileName(method, requestClass));
        PrintStream stream = null;
        ResponseResultWrapper wrapper;
        
        try {
            stream = new PrintStream(file);
            
            wrapper = new ResponseResultWrapper();
            if (response != null) {
                setResponseLine(response, wrapper);
                setResponseHeaders(response, wrapper);
                setResponseParameters(response, wrapper);
                setResponseEntity(method, test, response, responseData, wrapper, getFormatter(method));
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
        return getTestClass().getJavaClass().getName() + "-" + method.getName() + "-"
                + requestClass.getClass().getSimpleName() + ".response";
    }
    
    /**
     * Assigns the response body to the wrapper.
     */
    protected void setResponseEntity(FrameworkMethod method, Object test, HttpResponse response, byte[] responseData,
            ResponseResultWrapper wrapper, Class<? extends EntityFormatter> formatterType)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            Exception, IOException {
        EntityFormatter formatter;
        
        if (responseData != null) {
            if ((formatter = instance(method, test, formatterType)) != null) {
                wrapper.setEntity(formatter.format(response.getEntity(), responseData));
            } else {
                wrapper.setEntity(new String(responseData));
            }
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
    protected <T> T instance(FrameworkMethod method, Object test, Class<T> type) throws InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException,
            NoSuchMethodException {
        Constructor<T> constructor;
        
        if (type == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        
        if (type.getEnclosingClass() != null && type.getEnclosingClass().equals(getTestClass().getJavaClass())
                && !Modifier.isStatic(type.getModifiers())) {
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
        List<FrameworkMethod> methods = new ArrayList<FrameworkMethod>();
        Class<?>[] parameters;
        boolean valid;
        
        methods.addAll(getTestClass().getAnnotatedMethods(Doctest.class));
        methods.addAll(getTestClass().getAnnotatedMethods(SimpleDoctest.class));
        
        for (FrameworkMethod eachTestMethod : methods) {
            eachTestMethod.validatePublicVoid(false, errors);
            parameters = eachTestMethod.getMethod().getParameterTypes();
            
            valid = (parameters.length == 1 || parameters.length == 2)
                    && HttpResponse.class.isAssignableFrom(parameters[0]);
            
            if (!valid) {
                errors.add(new IllegalArgumentException(
                        "doctest methods needs to have the first parameter of type org.apache.http.HttpResponse and optionally a second parameter (representing the response-entity)."));
            }
        }
    }
    
    /**
     * Builds the request based on the specified data..
     */
    protected HttpRequestBase buildRequest(RequestData requestData) throws URISyntaxException {
        HttpRequestBase request = null;
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
    protected void invokeTestMethod(final FrameworkMethod method, final Object test, HttpResponse response,
            Class<?>[] methodParameters, byte[] responseData) throws Throwable, SAXException, IOException,
            ParserConfigurationException, JsonProcessingException {
        Object entity = null;
        ResponseDeserializer responseDeserializer = null;
        
        if (methodParameters.length == 1) {
            method.invokeExplosively(test, response);
        } else if (methodParameters.length == 2) {
            if (Document.class.isAssignableFrom(methodParameters[1])) {
                if (responseData == null || responseData.length == 0) {
                    method.invokeExplosively(test, response, null);
                } else {
                    method.invokeExplosively(test, response, DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .parse(new ByteArrayInputStream(responseData)));
                }
            } else if (JsonNode.class.isAssignableFrom(methodParameters[1]) && response.getEntity() != null) {
                if (responseData == null || responseData.length == 0) {
                    method.invokeExplosively(test, response, null);
                } else {
                    method.invokeExplosively(test, response, jsonMapper.readTree(responseData));
                }
            } else if (byte[].class.isAssignableFrom(methodParameters[1])) {
                method.invokeExplosively(test, response, responseData);
            } else if (CharSequence.class.isAssignableFrom(methodParameters[1])) {
                method.invokeExplosively(test, response, new String(responseData, response.getEntity()
                        .getContentEncoding().getValue()));
            } else {
                entity = null;
                responseDeserializer = responseDeserializers.get(extractContentType(response));
                if (responseDeserializer != null) {
                    entity = responseDeserializer.deserialize(responseData, methodParameters[1]);
                }
                method.invokeExplosively(test, response, entity);
            }
        }
    }
    
    private String extractContentType(HttpResponse response) {
        String contentType = "";
        int index = 0;
        
        if (response.getEntity() != null && response.getEntity().getContentType() != null) {
            contentType = response.getEntity().getContentType().getValue();
        }
        
        if (contentType != null && (index = contentType.indexOf(';')) != -1) {
            contentType = contentType.substring(0, index);
        }
        
        return contentType;
    }
    
    /**
     * Instances and configures the HTTP-client.
     */
    protected DefaultHttpClient getHttpClient(final RequestData requestData, final DoctestClient clientConfig,
            final RequestResultWrapper wrapper) {
        DefaultHttpClient client = new DefaultHttpClient();
        BasicCredentialsProvider credentialsProvider;
        HttpParams params = new BasicHttpParams();
        
        if (clientConfig == null || clientConfig.enableCompression()) {
            client.addRequestInterceptor(REQUEST_GZIP_INTERCEPTOR);
            client.addResponseInterceptor(RESPONSE_GZIP_INTERCEPTOR);
        }
        
        client.addRequestInterceptor(new HttpRequestInterceptor() {
            
            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                setRequestHeaders(request, wrapper);
                setRequestParameters(request, wrapper);
            }
            
        });
        
        if (requestData.getCredentials() != null) {
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, requestData.getCredentials());
            client.setCredentialsProvider(credentialsProvider);
        }
        
        if (clientConfig != null) {
            params.setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, clientConfig.allowCircularRedirects());
            params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, clientConfig.handleRedirects());
            params.setBooleanParameter(ClientPNames.REJECT_RELATIVE_REDIRECT, clientConfig.rejectRelativeRedirects());
            params.setIntParameter(ClientPNames.MAX_REDIRECTS, clientConfig.maxRedirects());
            client.setParams(params);
        }
        
        requestData.configureClient(client);
        
        return client;
    }
    
    /**
     * Extracts the requestData from the doctest-method.
     */
    protected RequestData[] getRequestData(final FrameworkMethod method, final Object test)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        RequestData[] requestData;
        final SimpleDoctest doctest;
        Class<? extends RequestData>[] requestClasses;
        
        if (method.getMethod().isAnnotationPresent(Doctest.class)) {
            requestClasses = method.getMethod().getAnnotation(Doctest.class).value();
            
            requestData = new RequestData[requestClasses.length];
            for (int i = 0; i < requestClasses.length; i++) {
                requestData[i] = instance(method, test, requestClasses[i]);
            }
        } else {
            doctest = method.getMethod().getAnnotation(SimpleDoctest.class);
            requestData = new RequestData[] { new AbstractRequestData() {
                
                @Override
                public URI getURI() throws URISyntaxException {
                    return new URI(doctest.value());
                }
                
                @Override
                public String getMethod() {
                    return doctest.method();
                }
                
                @Override
                public Header[] getHeaders() {
                    Header[] headers = new Header[doctest.header().length];
                    int index = 0, i;
                    
                    for (String header : doctest.header()) {
                        i = header.indexOf(':');
                        headers[index++] = new BasicHeader(header.substring(0, i).trim(), header.substring(i + 1)
                                .trim());
                    }
                    
                    return headers;
                };
                
            } };
        }
        
        return requestData;
    }
    
    /**
     * Performs a single request.
     */
    protected void executeRequest(final FrameworkMethod method, final HttpRequestBase request,
            final DoctestConfig doctestConfig, final DoctestConfig.AssertionMode assertionMode,
            final ThreadLocal<DefaultHttpClient> clients, ResponseContext responseCtx, RequestData requestClass)
            throws InterruptedException {
        DefaultHttpClient client = clients.get();
        int delay = doctestConfig == null ? 0 : doctestConfig.requestDelay();
        HttpResponse resp;
        byte[] tmp = null;
        CookieStore cookieStore = getCookieStore(method);
        HttpContext ctx = new BasicHttpContext();
        
        try {
            ctx.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            
            resp = client.execute(request, ctx);
            
            if (resp.getEntity() != null) {
                resp.setEntity(new BufferedHttpEntity(resp.getEntity()));
                tmp = EntityUtils.toByteArray(resp.getEntity());
            }
            
            assertExpectations(method, resp, requestClass);
            
            if (assertionMode == AssertionMode.FIRST && responseCtx.response == null) {
                responseCtx.response = resp;
                responseCtx.responseData = tmp;
            } else {
                responseCtx.response = resp;
                responseCtx.responseData = tmp;
            }
            // TODO: implement random
        } catch (Exception exception) {
            fail(exception.getLocalizedMessage());
        }
        
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }
    
    /**
     * Gets back the cookie store used by each request.
     */
    protected CookieStore getCookieStore(FrameworkMethod method) {
        DoctestCookieConfig config = method.getMethod().getAnnotation(DoctestCookieConfig.class);
        CookieStore cookieStore = defaultCookieStore;
        
        if (config != null) {
            if (config.name() != null && !config.name().trim().isEmpty()) {
                if (cookieStores.containsKey(config.name())) {
                    if (cookieStores.get(config.name()) == null) {
                        cookieStore = new BasicCookieStore();
                    } else {
                        cookieStore = cookieStores.get(config.name());
                    }
                }
            }
        }
        
        return cookieStore;
    }
    
    /**
     * Executes all the requests the doctest specifies.
     */
    protected void executeRequests(final FrameworkMethod method, final HttpRequestBase request,
            final DoctestConfig doctestConfig, final ThreadLocal<DefaultHttpClient> clients,
            final RequestData requestClass) {
        ExecutorService executorService;
        List<Future<Void>> futures;
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
        int requestCount;
        int maxConcurrent;
        final DoctestConfig.AssertionMode assertionMode;
        final ResponseContext responseCtx = responses.get();
        
        if (doctestConfig != null) {
            requestCount = doctestConfig.requestCount();
            maxConcurrent = doctestConfig.maxConcurrentRequests();
            assertionMode = doctestConfig.assertionMode();
        } else {
            requestCount = 1;
            maxConcurrent = 1;
            assertionMode = AssertionMode.LAST;
        }
        
        try {
            executorService = Executors.newFixedThreadPool(maxConcurrent);
            
            for (int i = 0; i < requestCount; i++) {
                tasks.add(new Callable<Void>() {
                    
                    @Override
                    public Void call() throws Exception {
                        executeRequest(method, request, doctestConfig, assertionMode, clients, responseCtx,
                                requestClass);
                        return null;
                    }
                    
                });
            }
            
            futures = executorService.invokeAll(tasks);
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.MINUTES);
            
            for (Future<Void> future : futures) {
                future.get(5, TimeUnit.MINUTES);
            }
        } catch (Exception exception) {
            fail(exception.getLocalizedMessage());
        }
    }
    
    /**
     * Executes the request.
     * 
     * @param method
     * @param test
     * @param requestData
     * @param clientConfig
     * @param doctestConfig
     * @throws URISyntaxException
     * @throws IOException
     * @throws Exception
     * @throws Throwable
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws JsonProcessingException
     */
    protected void executeTestcase(final FrameworkMethod method, final Object test, final RequestData requestData,
            final DoctestClient clientConfig, final DoctestConfig doctestConfig) throws URISyntaxException,
            IOException, Exception, Throwable, SAXException, ParserConfigurationException, JsonProcessingException {
        final HttpRequestBase request;
        final ThreadLocal<DefaultHttpClient> clients;
        final RequestResultWrapper wrapper = new RequestResultWrapper();
        Class<?>[] methodParameters = method.getMethod().getParameterTypes();
        byte[] requestEntityData = null;
        ResponseContext responseCtx = responses.get();
        HttpEntity entity;
        
        request = buildRequest(requestData);
        setRequestLine(request, wrapper);
        
        if (requestData.getHeaders() != null) {
            request.setHeaders(requestData.getHeaders());
        }
        
        if (requestData.getParameters() != null) {
            request.setParams(requestData.getParameters());
        }
        
        entity = requestData.getHttpEntity();
        if (entity != null && request instanceof HttpEntityEnclosingRequestBase) {
            if (!(entity instanceof MultipartEntity)) {
                requestEntityData = EntityUtils.toByteArray(entity);
            }
            ((HttpEntityEnclosingRequestBase) request).setEntity(entity);
        }
        
        clients = new ThreadLocal<DefaultHttpClient>() {
            
            @Override
            protected DefaultHttpClient initialValue() {
                return getHttpClient(requestData, clientConfig, wrapper);
            }
            
        };
        
        responseCtx.response = null;
        responseCtx.responseData = null;
        executeRequests(method, request, doctestConfig, clients, requestData);
        
        saveRequest(method, test, requestData, request, requestEntityData, wrapper);
        saveResponse(method, test, requestData, responseCtx.response, responseCtx.responseData);
        
        invokeTestMethod(method, test, responseCtx.response, methodParameters, responseCtx.responseData);
    }
    
}
