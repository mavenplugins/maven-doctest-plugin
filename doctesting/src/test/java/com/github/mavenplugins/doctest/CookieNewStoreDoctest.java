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

import static org.junit.Assert.assertNull;

import javax.servlet.http.Cookie;

import org.apache.http.HttpResponse;
import org.junit.runner.RunWith;

import com.github.mavenplugins.doctest.DoctestCookieConfig.Store;

@RunWith(DoctestRunner.class)
@DoctestCookieConfig(name = "configName1", store = Store.NEW)
public class CookieNewStoreDoctest {
    
    @SimpleDoctest("http://localhost:12345/cookie/name1/value1")
    public void newCookie1(HttpResponse response, Cookie[] cookies) throws Exception {
        assertNull(cookies);
    }
    
    @SimpleDoctest("http://localhost:12345/cookie/name2/value2")
    public void newCookie2(HttpResponse response, Cookie[] cookies) throws Exception {
        assertNull(cookies);
    }
    
}
