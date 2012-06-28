package com.github.mavenplugins.doctest;

import static com.jayway.restassured.RestAssured.given;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.Preferences;

import junit.framework.TestCase;

import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

public abstract class AbstractDoctest extends TestCase {
    
    public static final String TEST_SOURCE_PATH = "doctest.sources.path";
    public static final String RESULT_PATH = "doctest.result.path";
    
    protected Preferences prefs = Preferences.userNodeForPackage(AbstractDoctest.class);
    
    public abstract URL getURL() throws MalformedURLException;
    
    public abstract Method getHttpMethod();
    
    public abstract void validateResponse(Response response);
    
    public void buildRequest(RequestSpecBuilder requestBuilder) {
    }
    
    public void buildExpectedResponse(ResponseSpecBuilder responseBuilder) {
        responseBuilder.expectStatusCode(200);
    }
    
    @Test
    public void testWebService() throws MalformedURLException {
        RequestSpecBuilder requestBuilder = new RequestSpecBuilder();
        ResponseSpecBuilder responseBuilder = new ResponseSpecBuilder();
        RequestSpecification request;
        ResponseSpecification expectedResponse;
        URL url = getURL();
        Response response;
        
        RestAssured.port = url.getPort();
        RestAssured.baseURI = url.getProtocol() + "://" + url.getHost();
        
        buildRequest(requestBuilder);
        buildExpectedResponse(responseBuilder);
        
        System.out.println(prefs.get(TEST_SOURCE_PATH, "TEST_SOURCE_PATH"));
        System.out.println(prefs.get(RESULT_PATH, "RESULT_PATH"));
        
        request = requestBuilder.build();
        expectedResponse = responseBuilder.build();
        
        switch (getHttpMethod()) {
            case DELETE:
                response = given(request, expectedResponse).delete(url.getFile());
                break;
            case HEAD:
                response = given(request, expectedResponse).head(url.getFile());
                break;
            case POST:
                response = given(request, expectedResponse).post(url.getFile());
                break;
            case PUT:
                response = given(request, expectedResponse).put(url.getFile());
                break;
            default:
            case GET:
                response = given(request, expectedResponse).get(url.getFile());
                break;
        }
        
        validateResponse(response);
    }
    
}
