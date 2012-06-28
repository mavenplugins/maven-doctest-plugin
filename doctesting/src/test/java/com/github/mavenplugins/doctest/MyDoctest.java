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

import java.net.MalformedURLException;
import java.net.URL;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.response.Response;

public class MyDoctest extends AbstractDoctest {
    
    /*
     * SERVER FOR TESTING (JUST AN EXAMPLE)
     */
    
    @WebService
    public static class SimpleService {
        
        public int multiply(int x, int y) {
            return x * y;
        }
        
    }
    
    Endpoint endpoint;
    
    @Override
    protected void setUp() throws Exception {
        SimpleService service = new SimpleService();
        endpoint = Endpoint.publish("http://localhost:8080/multiply", service);
    }
    
    @Override
    protected void tearDown() throws Exception {
        endpoint.stop();
    }
    
    /*
     * SERVER FOR TESTING (JUST AN EXAMPLE)
     */
    
    @Override
    public Method getHttpMethod() {
        return Method.GET;
    }
    
    @Override
    public URL getURL() throws MalformedURLException {
        return new URL("http://localhost:8080/multiply?wsdl");
    }
    
    @Override
    public void validateResponse(Response response) {
        System.out.println("response: " + response.contentType());
    }
    
}
