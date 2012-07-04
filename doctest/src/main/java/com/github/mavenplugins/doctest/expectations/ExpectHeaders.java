package com.github.mavenplugins.doctest.expectations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotation used to automatically verify the correct headers.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExpectHeaders {
    
    /**
     * the headers to verify.
     */
    ExpectHeader[] value();
    
}
