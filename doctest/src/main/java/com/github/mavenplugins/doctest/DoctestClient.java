package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apache HTTP client configuration annotation for a doctest.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface DoctestClient {
    
    /**
     * Determines if the HTTP-client should follow redirects.
     */
    boolean handleRedirects() default true;
    
    /**
     * Determines if the HTTP-client should allow relative redirect locations.
     */
    boolean rejectRelativeRedirects() default false;
    
    /**
     * Determines if the HTTP-client should allow cyclic redirects.
     */
    boolean allowCircularRedirects() default true;
    
    /**
     * Determines the max. redirect-count.
     */
    int maxRedirects() default 100;
    
    /**
     * Allow compression or not.
     */
    boolean enableCompression() default true;
    
}
