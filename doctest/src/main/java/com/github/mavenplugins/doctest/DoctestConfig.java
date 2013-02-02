package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration for doctest execution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface DoctestConfig {
    
    /**
     * This enum is used to determine which response is given to the test-method.
     */
    public enum AssertionMode {
        /**
         * The first incoming response.
         */
        FIRST,
        /**
         * Any response can be given to the test-method.
         */
        RANDOM,
        /**
         * The last incoming response.
         */
        LAST
    }
    
    /**
     * Determines how many requests should be performed.
     */
    int requestCount() default 1;
    
    /**
     * Determines how many connections (and threads) should be used.
     */
    int maxConcurrentRequests() default 1;
    
    /**
     * Determines the delay between the requests per thread in milliseconds.
     */
    int requestDelay() default 0;
    
    /**
     * Defines the assertionMode.
     */
    AssertionMode assertionMode() default AssertionMode.LAST;
    
}
