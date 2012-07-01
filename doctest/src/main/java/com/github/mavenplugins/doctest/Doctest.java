package com.github.mavenplugins.doctest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.mavenplugins.doctest.formatter.EntityFormatter;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface Doctest {
    
    Class<? extends RequestData> value();
    
    Class<? extends EntityFormatter> formatter() default EntityFormatter.class;
    
}
