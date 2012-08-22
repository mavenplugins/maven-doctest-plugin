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
    
    public enum AssertionMode {
        FIRST, RANDOM, LAST
    }
    
    int requestCount() default 1;
    
    int maxConcurrentRequests() default 1;
    
    int requestDelay() default 0;
    
    AssertionMode assertionMode() default AssertionMode.LAST;
    
}
