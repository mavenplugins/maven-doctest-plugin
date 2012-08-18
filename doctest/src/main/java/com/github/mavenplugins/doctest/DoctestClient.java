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
    
    boolean handleRedirects() default true;
    
    boolean rejectRelativeRedirects() default false;
    
    boolean allowCircularRedirects() default true;
    
    int maxRedirects() default 100;
    
    boolean enableCompression() default true;
    
}
