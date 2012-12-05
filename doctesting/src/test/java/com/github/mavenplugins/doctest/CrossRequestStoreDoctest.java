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

import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.junit.runner.RunWith;

import com.github.mavenplugins.doctest.DoctestStore.Source;
import com.github.mavenplugins.doctest.expectations.ExpectHeader;
import com.github.mavenplugins.doctest.expectations.ExpectStatus;

@RunWith(DoctestRunner.class)
@DoctestHost(host = "localhost", port = 12345)
public class CrossRequestStoreDoctest {
    
    @SimpleDoctest("/cross-request/setHeader")
    @DoctestStore(id = "myHeaderVar", expression = "X-Header", source = Source.HEADER)
    @DoctestOrder(1)
    public void setHeader(HttpResponse response) {
    }
    
    @SimpleDoctest(value = "/cross-request/withHeader/${myHeaderVar.value}", header = "${myHeaderVar.name}: ${myHeaderVar.value}")
    @ExpectStatus(204)
    @DoctestOrder(2)
    public void withHeader(HttpResponse response, DoctestContext ctx) {
        assertEquals("X-Header-Value", ((Header) ctx.getValue("myHeaderVar")).getValue());
    }
    
    @SimpleDoctest("/cross-request/setCookie")
    @DoctestStore(id = "myCookieVar", expression = "X-Cookie", source = Source.COOKIE)
    @DoctestOrder(3)
    public void setCookie(HttpResponse response) {
    }
    
    @SimpleDoctest(value = "/cross-request/withCookie/${myCookieVar.value}", header = "Cookie: ${myCookieVar.name}=${myCookieVar.value}")
    @ExpectStatus(204)
    @DoctestOrder(4)
    public void withCookie(HttpResponse response, DoctestContext ctx) {
        assertEquals("X-Cookie-Value", ((Cookie) ctx.getValue("myCookieVar")).getValue());
    }
    
    @SuppressWarnings("unchecked")
    @SimpleDoctest("/cross-request/getContent")
    @DoctestStore(id = "myContentVar", source = Source.CONTENT)
    @ExpectHeader(name = HttpHeaders.CONTENT_TYPE, content = "application/json.*")
    @ExpectStatus(200)
    @DoctestOrder(5)
    public void getContent(HttpResponse response, DoctestContext ctx) {
        assertEquals("X-FirstName", ((Map<String, Object>) ctx.getValue("myContentVar")).get("firstName"));
        assertEquals("X-LastName", ((Map<String, Object>) ctx.getValue("myContentVar")).get("lastName"));
    }
    
    @SuppressWarnings("unchecked")
    @SimpleDoctest(value = "/cross-request/withContent/${myContentVar.firstName}/${myContentVar.lastName}")
    @DoctestOrder(6)
    public void withContent(HttpResponse response, DoctestContext ctx) {
        assertEquals("X-FirstName", ((Map<String, Object>) ctx.getValue("myContentVar")).get("firstName"));
        assertEquals("X-LastName", ((Map<String, Object>) ctx.getValue("myContentVar")).get("lastName"));
    }
    
    @SimpleDoctest("/cross-request/setHeader")
    @DoctestStore(id = "myVar", expression = "X-Header", source = Source.HEADER)
    @DoctestOrder(8)
    public void overrideHeader1(HttpResponse response, DoctestContext ctx) {
        assertEquals("X-Header-Value", ((Header) ctx.getValue("myVar")).getValue());
    }
    
    @SimpleDoctest("/cross-request/setCookie")
    @DoctestStore(id = "myVar", expression = "X-Cookie", source = Source.COOKIE)
    @DoctestOrder(9)
    public void overrideHeader2(HttpResponse response, DoctestContext ctx) {
        assertEquals("X-Cookie-Value", ((Cookie) ctx.getValue("myVar")).getValue());
    }
    
}
