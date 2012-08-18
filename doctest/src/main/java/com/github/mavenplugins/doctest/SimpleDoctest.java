package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.http.client.methods.HttpGet;

import com.github.mavenplugins.doctest.formatter.EntityFormatter;

/**
 * Annotation for simple doctests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface SimpleDoctest {
    
    /**
     * Gets the request url.
     */
    String value();
    
    /**
     * Gets the request headers (header/value pairs).
     */
    String[] header() default {};
    
    /**
     * Gets the request method.
     */
    String method() default HttpGet.METHOD_NAME;
    
    /**
     * Gets the formatter for the response.
     */
    Class<? extends EntityFormatter> formatter() default EntityFormatter.class;
    
}
