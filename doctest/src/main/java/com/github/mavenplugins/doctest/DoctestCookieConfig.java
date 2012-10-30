package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for per test-class cookie-handling.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface DoctestCookieConfig {
    
    /**
     * The store method for cookies.
     */
    public enum Store {
        /**
         * Shares all cookies for each request with the same cookieConfig name.
         */
        SHARED,
        /**
         * Each request means a new cookie store - no cookie is shared between subsequent requests.
         */
        NEW
    }
    
    /**
     * The name of the config.
     */
    String name();
    
    /**
     * The type of store for each request.
     */
    Store store();
    
}
