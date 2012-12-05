package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Basic cross-request store config.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface DoctestStore {
    
    /**
     * The source of value to store.
     */
    public enum Source {
        /**
         * Specifies cookies as source for values - the value assigned is of type org.apache.http.cookie.Cookie.
         */
        COOKIE,
        /**
         * Specifies response-header as source for values - the value assigned is of type org.apache.http.Header.
         */
        HEADER,
        /**
         * Specifies the content as source for values.
         */
        CONTENT
    }
    
    /**
     * The identifier for the header value.
     */
    String id();
    
    /**
     * The expression for the cookie or header (the content don't need an expression for identification) - can be a RegExp.
     */
    String expression() default "";
    
    /**
     * The source of the value to store.
     */
    Source source() default Source.HEADER;
    
}
