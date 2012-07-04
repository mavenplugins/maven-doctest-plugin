package com.github.mavenplugins.doctest.expectations;

/**
 * A single header (key / value) representation.
 */
public @interface ExpectHeader {
    
    /**
     * The name of the header.
     */
    String name();
    
    /**
     * A regExp used to determine the correct header value.
     */
    String content() default "";
    
}
