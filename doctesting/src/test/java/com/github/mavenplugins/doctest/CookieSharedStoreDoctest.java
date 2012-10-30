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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.http.HttpResponse;
import org.junit.runner.RunWith;

import com.github.mavenplugins.doctest.DoctestCookieConfig.Store;

@RunWith(DoctestRunner.class)
public class CookieSharedStoreDoctest {
    
    @SimpleDoctest("http://localhost:12345/cookie/name1/value1")
    @DoctestCookieConfig(name = "config1", store = Store.NEW)
    @DoctestOrder(0)
    public void newCookie1(HttpResponse response, Cookie[] cookies) throws Exception {
        assertNull(cookies);
    }
    
    @SimpleDoctest("http://localhost:12345/cookie/name2/value2")
    @DoctestCookieConfig(name = "config1", store = Store.NEW)
    @DoctestOrder(0)
    public void newCookie2(HttpResponse response, Cookie[] cookies) throws Exception {
        assertNull(cookies);
    }
    
    @SimpleDoctest("http://localhost:12345/cookie/name3/value3")
    @DoctestOrder(1)
    public void shareCookie1(HttpResponse response, Cookie[] cookies) throws Exception {
        assertNull(cookies);
    }
    
    @SimpleDoctest("http://localhost:12345/cookie/name4/value4")
    @DoctestOrder(2)
    public void shareCookie2(HttpResponse response, Map<String, Object>[] cookies) throws Exception {
        assertNotNull(cookies);
        assertEquals(1, cookies.length);
        assertEquals("name3", cookies[0].get("name"));
        assertEquals("value3", cookies[0].get("value"));
    }
    
    @SimpleDoctest("http://localhost:12345/cookie/name5/value5")
    @DoctestOrder(3)
    public void shareCookie3(HttpResponse response, Map<String, Object>[] cookies) throws Exception {
        assertNotNull(cookies);
        assertEquals(2, cookies.length);
        assertEquals("name3", cookies[0].get("name"));
        assertEquals("value3", cookies[0].get("value"));
        assertEquals("name4", cookies[1].get("name"));
        assertEquals("value4", cookies[1].get("value"));
    }
    
    @SimpleDoctest("http://localhost:12345/cookie/name6/value6")
    @DoctestOrder(1)
    @DoctestCookieConfig(name = "config2", store = Store.SHARED)
    public void shareOtherCookie1(HttpResponse response, Cookie[] cookies) throws Exception {
        assertNull(cookies);
    }
    
    @SimpleDoctest("http://localhost:12345/cookie/name7/value7")
    @DoctestOrder(2)
    @DoctestCookieConfig(name = "config2", store = Store.SHARED)
    public void shareOtherCookie2(HttpResponse response, Map<String, Object>[] cookies) throws Exception {
        assertNotNull(cookies);
        assertEquals(1, cookies.length);
        assertEquals("name6", cookies[0].get("name"));
        assertEquals("value6", cookies[0].get("value"));
    }
    
}
