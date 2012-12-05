package com.github.mavenplugins.doctest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class ReflectionUtil {
    
    /**
     * The doctest class.
     */
    protected Class<?> testClass;
    
    /**
     * Initiates the util with the specified test class.
     */
    public void init(Class<?> testClass) {
        this.testClass = testClass;
    }
    
    /**
     * Instantiates the given type.
     */
    protected <T> T instance(Object test, Class<T> type) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchMethodException {
        Constructor<T> constructor;
        
        if (type == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        
        if (type.getEnclosingClass() != null && type.getEnclosingClass().equals(testClass)
                && !Modifier.isStatic(type.getModifiers())) {
            constructor = type.getDeclaredConstructor(testClass);
            
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            
            return constructor.newInstance(test);
        } else {
            constructor = type.getDeclaredConstructor();
            
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            
            return constructor.newInstance();
        }
    }
    
}
