package com.github.mavenplugins.doctest.expectations;

public @interface ExpectHeader {
    
    String name();
    
    String content() default "";
    
}
