package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the default host for a doctest class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface DoctestHost {
    
    /**
     * The host - even a DNS entry or an IP Address - defaults to localhost.
     */
    String host() default "localhost";
    
    /**
     * The port for the doctest - defaults to -1.
     */
    int port() default -1;
    
    /**
     * The scheme / protocol for the doctest - defaults to http.
     */
    String scheme() default "http";
    
    /**
     * The contextPath of you webapp - defaults to /.
     */
    String contextPath() default "/";
    
}
