package com.github.mavenplugins.doctest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Header;
import org.apache.http.cookie.Cookie;

/**
 * Parameter / Variable holder for doctests.
 */
public class DoctestContext {
    
    /**
     * Stores variables from requests.
     */
    protected Map<String, Object> store = new ConcurrentHashMap<String, Object>();
    
    /**
     * Gets the raw store back.
     */
    public Map<String, Object> getStore() {
        return store;
    }
    
    /**
     * Returns the casted value form the store.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String id) {
        return (T) store.get(id);
    }
    
    /**
     * Stores the values from the response to the value-store (Map).
     */
    public void apply(Method testMethod, ResponseContext ctx) {
        DoctestStores doctestStoresAnnotations = testMethod.getAnnotation(DoctestStores.class);
        DoctestStore doctestStoreAnnotation = testMethod.getAnnotation(DoctestStore.class);
        DoctestStore doctestStores[] = null;
        
        if (doctestStoresAnnotations != null) {
            doctestStores = doctestStoresAnnotations.value();
        } else if (doctestStoreAnnotation != null) {
            doctestStores = new DoctestStore[] { doctestStoreAnnotation };
        }
        
        if (doctestStores != null) {
            for (DoctestStore doctestStore : doctestStores) {
                switch (doctestStore.source()) {
                    case CONTENT:
                        store.put(doctestStore.id(), ctx.getEntity());
                        break;
                    case COOKIE:
                        for (Cookie cookie : ctx.getCookieStore().getCookies()) {
                            if (cookie.getName().matches(doctestStore.expression())) {
                                store.put(doctestStore.id(), cookie);
                            }
                        }
                        break;
                    default:
                    case HEADER:
                        for (Header header : ctx.getResponse().getAllHeaders()) {
                            if (header.getName().matches(doctestStore.expression())) {
                                store.put(doctestStore.id(), header);
                            }
                        }
                        break;
                }
            }
        }
    }
    
}
