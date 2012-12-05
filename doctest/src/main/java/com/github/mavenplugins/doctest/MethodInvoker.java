package com.github.mavenplugins.doctest;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.junit.runners.model.FrameworkMethod;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Util for invoking the test-method.
 */
public class MethodInvoker {
    
    /**
     * Cross-request store.
     */
    protected DoctestContext store;
    
    /**
     * Initiates the util with the specified test class.
     */
    public void init(DoctestContext store) {
        this.store = store;
    }
    
    /**
     * Actually runs the test method.
     */
    protected void invokeTestMethod(final FrameworkMethod method, final Object test, ResponseContext ctx)
            throws Throwable, SAXException, IOException, ParserConfigurationException, JsonProcessingException {
        HttpResponse response = ctx.getResponse();
        Class<?>[] methodParameters = method.getMethod().getParameterTypes();
        
        if (methodParameters.length == 1) {
            method.invokeExplosively(test, response);
        } else if (methodParameters.length == 2) {
            if (DoctestContext.class.isAssignableFrom(methodParameters[1])) {
                method.invokeExplosively(test, response, store);
            } else {
                method.invokeExplosively(test, response, ctx.getEntity());
            }
        } else if (methodParameters.length == 3) {
            if (DoctestContext.class.isAssignableFrom(methodParameters[1])) {
                method.invokeExplosively(test, response, store, ctx.getEntity());
            } else {
                method.invokeExplosively(test, response, ctx.getEntity(), store);
            }
        }
    }
    
}
