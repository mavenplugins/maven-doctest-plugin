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

import java.net.URI;
import java.net.URISyntaxException;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import com.github.mavenplugins.doctest.formatter.XmlPrettyPrinter;

@RunWith(DoctestRunner.class)
public class MyDoctest {
    
    class Resource1 extends AbstractRequestData {
        
        public URI getURI() throws URISyntaxException {
            return new URI("http://localhost:8080/someService?wsdl");
        }
        
    }
    
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
     * My Doctest doc.
     * new line
     * 
     * 
     * @param custom
     *            annotation
     */
    @Doctest(value = Resource1.class, formatter = XmlPrettyPrinter.class)
    public void resource1(HttpResponse response, Document document) throws Exception {
    }
    
    @Doctest(value = Resource1.class, formatter = XmlPrettyPrinter.class)
    public void resource2(HttpResponse response, Document document) throws Exception {
    }
    
    @Doctest(value = Resource1.class, formatter = XmlPrettyPrinter.class)
    public void resource3(HttpResponse response, Document document) throws Exception {
    }
    
    @Doctest(value = Resource1.class, formatter = XmlPrettyPrinter.class)
    public void resource4(HttpResponse response, Document document) throws Exception {
    }
    
}
