package com.github.mavenplugins.doctest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.github.mavenplugins.doctest.DoctestConfig.AssertionMode;
import com.github.mavenplugins.doctest.DoctestCookieConfig.Store;
import com.github.mavenplugins.doctest.asserts.HttpResponseAssertUtils;
import com.github.mavenplugins.doctest.expectations.ExpectHeader;
import com.github.mavenplugins.doctest.expectations.ExpectHeaders;
import com.github.mavenplugins.doctest.expectations.ExpectStatus;

/**
 * A TestRunner only for doctests.
 */
public class DoctestRunner extends BlockJUnit4ClassRunner {
    
    /**
     * The java backstore variable for the sources of the doctests.
     */
    public static final String TEST_SOURCE_PATH = "doctest.sources.path";
    /**
     * Enable the gzip.
     */
    private static final RequestAcceptEncoding REQUEST_GZIP_INTERCEPTOR = new RequestAcceptEncoding();
    /**
     * Enable the gzip.
     */
    private static final ResponseContentEncoding RESPONSE_GZIP_INTERCEPTOR = new ResponseContentEncoding();
    
    /**
     * context object for asynchronous request execution.
     */
    protected ThreadLocal<ResponseContext> responses = new ThreadLocal<ResponseContext>() {
        
        @Override
        protected ResponseContext initialValue() {
            return new ResponseContext();
        }
        
    };
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
     * Cross-request store.
     */
    protected DoctestContext store = new DoctestContext();
    /**
     * helper for request objects.
     */
    protected RequestBuilder requestBuilder = new RequestBuilder();
    /**
     * Util for instancing classes.
     */
    protected ReflectionUtil reflectionUtil = new ReflectionUtil();
    /**
     * helper for reporting data.
     */
    protected ReportingCollector reportingCollector = new ReportingCollector();
    /**
     * helper for invoking the test-method.
     */
    protected MethodInvoker methodInvoker = new MethodInvoker();
    /**
     * helper for response entity deserialization.
     */
    protected ResponseDeserializerUtil responseDeserializerUtil = new ResponseDeserializerUtil();
    
    /**
     * constructs the runner with the given test class.
     * 
     * @throws URISyntaxException May throws an URISyntaxException when instantiating the default URI.
     */
    public DoctestRunner(Class<?> testClass) throws InvocationTargetException, InitializationError, URISyntaxException {
        super(testClass);
        
        requestBuilder.init(getTestClass().getJavaClass());
        reflectionUtil.init(getTestClass().getJavaClass());
        reportingCollector.init(getTestClass().getJavaClass());
        methodInvoker.init(store);
        responseDeserializerUtil.init();
        
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
    
    @Override
    public void run(RunNotifier notifier) {
        try {
            reportingCollector.testRunStarted(null);
            super.run(notifier);
            reportingCollector.testRunFinished(null);
        } catch (Exception exception) {
            fail(exception.getLocalizedMessage());
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
                
                requestData = requestBuilder.getRequestData(method, test, store);
                
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
            
            valid = (parameters.length >= 1 && parameters.length <= 3)
                    && HttpResponse.class.isAssignableFrom(parameters[0]);
            
            if (!valid) {
                errors.add(new IllegalArgumentException(
                        "Doctest methods needs to have the first parameter of type org.apache.http.HttpResponse and optionally a second / third parameter (DoctestContext and / or the response-entity)."));
            }
        }
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
                reportingCollector.setRequestHeaders(request, wrapper);
                reportingCollector.setRequestParameters(request, wrapper);
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
     * Performs a single request.
     */
    protected void executeRequest(FrameworkMethod method, HttpRequestBase request, DoctestConfig doctestConfig,
            DoctestClient doctestClient, ResponseContext responseCtx, RequestData requestClass,
            RequestResultWrapper wrapper) throws InterruptedException {
        int delay = doctestConfig == null ? 0 : doctestConfig.requestDelay();
        HttpResponse resp;
        byte[] tmp = null;
        CookieStore cookieStore = getCookieStore(method);
        HttpContext ctx = new BasicHttpContext();
        
        try {
            ctx.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            responseCtx.setHttpContext(ctx);
            responseCtx.setCookieStore(cookieStore);
            responseCtx.setHttpClient(getHttpClient(requestClass, doctestClient, wrapper));
            
            resp = responseCtx.getHttpClient().execute(request, ctx);
            
            if (resp.getEntity() != null) {
                resp.setEntity(new BufferedHttpEntity(resp.getEntity()));
                tmp = EntityUtils.toByteArray(resp.getEntity());
            }
            
            assertExpectations(method, resp, requestClass);
            
            if (doctestConfig != null && doctestConfig.assertionMode() == AssertionMode.FIRST
                    && responseCtx.response == null) {
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
     * Executes all the requests the doctest specifies.
     */
    protected void executeRequests(final FrameworkMethod method, final HttpRequestBase request,
            final DoctestConfig doctestConfig, final DoctestClient doctestClient, final RequestData requestClass,
            final RequestResultWrapper wrapper) {
        ExecutorService executorService;
        List<Future<Void>> futures;
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
        int requestCount;
        int maxConcurrent;
        final ResponseContext responseCtx = responses.get();
        
        if (doctestConfig != null) {
            requestCount = doctestConfig.requestCount();
            maxConcurrent = doctestConfig.maxConcurrentRequests();
        } else {
            requestCount = 1;
            maxConcurrent = 1;
        }
        
        try {
            executorService = Executors.newFixedThreadPool(maxConcurrent);
            
            for (int i = 0; i < requestCount; i++) {
                tasks.add(new Callable<Void>() {
                    
                    @Override
                    public Void call() throws Exception {
                        executeRequest(method, request, doctestConfig, doctestClient, responseCtx, requestClass,
                                wrapper);
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
     */
    protected void executeTestcase(FrameworkMethod method, Object test, RequestData requestData,
            DoctestClient doctestClient, DoctestConfig doctestConfig) throws Throwable {
        HttpRequestBase request;
        RequestResultWrapper wrapper = new RequestResultWrapper();
        byte[] requestEntityData = null;
        ResponseContext responseCtx = responses.get();
        HttpEntity entity;
        
        request = requestBuilder.buildRequest(requestData, method.getMethod(), store);
        reportingCollector.setRequestLine(request, wrapper);
        
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
        
        responseCtx.setResponse(null);
        responseCtx.setResponseData(null);
        responseCtx.setEntity(null);
        
        executeRequests(method, request, doctestConfig, doctestClient, requestData, wrapper);
        
        responseDeserializerUtil.deserialize(responseCtx, responseDeserializerUtil.getEntityType(method.getMethod()));
        reportingCollector.saveRequest(method, test, requestData, request, requestEntityData, wrapper);
        reportingCollector.saveResponse(method, test, requestData, responseCtx);
        store.apply(method.getMethod(), responseCtx);
        
        methodInvoker.invokeTestMethod(method, test, responseCtx);
    }
    
}
