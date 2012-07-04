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

import com.github.mavenplugins.doctest.expectations.ExpectHeader;
import com.github.mavenplugins.doctest.expectations.ExpectHeaders;
import com.github.mavenplugins.doctest.expectations.ExpectStatus;
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
    
    @Doctest(value = Resource1.class, formatter = XmlPrettyPrinter.class)
    @ExpectHeaders({ @ExpectHeader(name = "Content-Type", content = "text/.*") })
    @ExpectStatus(200)
    public void resource1(HttpResponse response, Document document) throws Exception {
    }
    
}
