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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mavenplugins.doctest.asserts.JsonAssertUtils;
import com.github.mavenplugins.doctest.expectations.ExpectHeader;
import com.github.mavenplugins.doctest.expectations.ExpectHeaders;
import com.github.mavenplugins.doctest.expectations.ExpectStatus;
import com.github.mavenplugins.doctest.formatter.JsonPrettyPrinter;
import com.github.mavenplugins.doctest.formatter.XmlPrettyPrinter;

@RunWith(DoctestRunner.class)
public class ShowcaseDoctest {
    
    class WSDLDescriptor extends AbstractRequestData {
        
        public URI getURI() throws URISyntaxException {
            return new URI("http://localhost:8080/someService?wsdl");
        }
        
    }
    
    class Jack extends AbstractRequestData {
        
        public URI getURI() throws URISyntaxException {
            return new URI("http://localhost:12345/user/jack");
        }
        
    }
    
    class Johnny extends AbstractRequestData {
        
        public URI getURI() throws URISyntaxException {
            return new URI("http://localhost:12345/user/johnny");
        }
        
    }
    
    class BadJohnny extends Johnny {
        
        @Override
        public String getMethod() {
            return HttpPut.METHOD_NAME;
        }
        
    }
    
    class PutJohnny extends AbstractRequestData {
        
        public URI getURI() throws URISyntaxException {
            return new URI("http://localhost:12345/user/setJohnny");
        }
        
        @Override
        public String getMethod() {
            return HttpPut.METHOD_NAME;
        }
        
        @Override
        public HttpEntity getHttpEntity() {
            return new ByteArrayEntity(
                    "{\"firstName\":\"Jack\",\"lastName\" : \"Daniels\",\"birthday\" : 1341437232926,\"address\" : {  \"street\" : \"Main Ave.\",  \"number\" : \"7A\",  \"city\" : \"New York\",  \"zipcode\" : \"7A1234\",  \"country\" : \"USA\"},\"friends\" : [ {  \"firstName\" : \"Freddy\",  \"lastName\" : \"Johnson\",  \"birthday\" : 1341437232926,  \"address\" : null,  \"friends\" : [ ],  \"friendshipSince\" : 1341437232926} ]}"
                            .getBytes(), ContentType.APPLICATION_JSON);
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
     * This request should get us a valid WSDL descriptor.
     */
    @Doctest(value = WSDLDescriptor.class, formatter = XmlPrettyPrinter.class)
    public void myXmlTest(HttpResponse response, Document document) throws Exception {
        
    }
    
    @Doctest(value = Jack.class, formatter = JsonPrettyPrinter.class)
    public void myJsonTest(HttpResponse response, JsonNode document) throws Exception {
    }
    
    /**
     * Johnny knows Jack and his friend. But he is not directly a friend of Jack's friend ...
     */
    @Doctest(value = Johnny.class, formatter = JsonPrettyPrinter.class)
    @ExpectStatus(200)
    @ExpectHeaders({ @ExpectHeader(name = "Content-Type", content = "application/json.*") })
    public void myOtherJsonTest(HttpResponse response, JsonNode document) throws Exception {
        JsonAssertUtils.assertExists("Johnny is Jacks friend, and Jack is Freddy's friend", document, "//*");
        //assertEquals("Johnny is not directly Freddy's friend", 0, JsonAssertUtils.count(document, "//*"));
    }
    
    /**
     * This Test should fail, because we use the wrong http method
     */
    @Doctest(value = BadJohnny.class, formatter = JsonPrettyPrinter.class)
    @ExpectStatus(405)
    public void badHttpMethod(HttpResponse response, JsonNode document) throws Exception {
    }
    
    @Doctest(value = PutJohnny.class, formatter = JsonPrettyPrinter.class)
    public void putMethod(HttpResponse response) throws Exception {
    }
    
}
