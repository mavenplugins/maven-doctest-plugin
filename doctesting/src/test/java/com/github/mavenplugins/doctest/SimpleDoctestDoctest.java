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

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

@RunWith(DoctestRunner.class)
public class SimpleDoctestDoctest {
    
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
    
    @Before
    public void startServer() throws Exception {
        SimpleService service = new SimpleService();
        endpoint = Endpoint.publish("http://localhost:8080/someService", service);
    }
    
    @After
    public void stopServer() throws Exception {
        endpoint.stop();
    }
    
    /*
     * SERVER FOR TESTING (JUST AN EXAMPLE)
     */
    
    /**
     * This request should get us a valid WSDL descriptor using the {@link SimpleDoctest} annotation.
     */
    @SimpleDoctest("http://localhost:8080/someService?wsdl")
    public void simpleDoctestWithResponse(HttpResponse response) throws Exception {
    }
    
    /**
     * This request should get us the response body as byte array.
     */
    @SimpleDoctest("http://localhost:8080/someService?wsdl")
    public void simpleDoctestWithByteArray(HttpResponse response, byte[] entity) throws Exception {
    }
    
    /**
     * This request should get us the response body as xml document.
     */
    @SimpleDoctest("http://localhost:8080/someService?wsdl")
    public void simpleDoctestWithXMLDocument(HttpResponse response, Document document) throws Exception {
    }
    
}
