package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for the order of doctest in the same testclass.
 * A lesser value will be executed earlier that a higher value.
 * Doctest methods without an order will be treated as they have the value 0.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface DoctestOrder {
    
    /**
     * The order value.
     */
    int value() default 0;
    
}
