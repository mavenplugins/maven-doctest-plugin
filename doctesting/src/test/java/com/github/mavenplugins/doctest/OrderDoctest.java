/**
 * Copyright 2012 the contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.github.mavenplugins.doctest;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.junit.runner.RunWith;

import com.github.mavenplugins.doctest.DoctestConfig.AssertionMode;

@RunWith(DoctestRunner.class)
public class OrderDoctest {
    
    /**
     * the second (64 concurrent users doing 1024 requests - the last response is passed to this method).
     */
    @SimpleDoctest("http://localhost:12345/order")
    @DoctestConfig(maxConcurrentRequests = 64, requestCount = 1024, requestDelay = 1, assertionMode = AssertionMode.LAST)
    public void zero(HttpResponse response, byte[] entity) throws Exception {
        assertEquals("1024", new String(entity));
    }
    
    /**
     * the first.
     */
    @SimpleDoctest("http://localhost:12345/order")
    @DoctestOrder(-1)
    public void first(HttpResponse response, byte[] entity) throws Exception {
        assertEquals("0", new String(entity));
    }
    
    /**
     * the last test method.
     */
    @SimpleDoctest("http://localhost:12345/order")
    @DoctestOrder(Integer.MAX_VALUE)
    public void last(HttpResponse response, byte[] entity) throws Exception {
        assertEquals("1025", new String(entity));
    }
    
}
