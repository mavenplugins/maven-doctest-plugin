package com.github.mavenplugins.doctest.expectations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotation used to automatically verify the correct response code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExpectStatus {
    
    /**
     * The http response status code.
     */
    int value();
    
    /**
     * A regExp representing the response message.
     */
    String message() default "";
    
}
