# Maven Doctest Plugin - Python-like Doctesting for Java
http://mavenplugins.github.com/maven-doctest-plugin/

Writing tests for you REST API that also documents it by example is a great asset in the python world.
This project tries to enable java developers to benefit from the same testing comfort and documentation mechanism.

# License

* Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

# Requirements / Dependencies

* Java 1.6+ (http://www.java.com/de/download/)
* JUnit 4+ (http://www.junit.org/)
* Jackson 2+ (https://github.com/FasterXML/jackson-core/)

# How to get it

The maven dependency for doctesting

```xml
<dependency>
    <groupId>com.github.mavenplugins.maven-doctest-plugin</groupId>
    <artifactId>doctest</artifactId>
    <version>1.8.1</version>
    <scope>test</scope>
</dependency>
```

The maven reporting-plugin

```xml
<reporting>
    <plugins>
    	...
        <plugin>
            <groupId>com.github.mavenplugins.maven-doctest-plugin</groupId>
            <artifactId>doctest-plugin</artifactId>
            <version>1.8.1</version>
        </plugin>
        ...
    </plugins>
</reporting>
```

And the corresponding repository

```xml
<repositories>
    <repository>
        <id>sonatype-releases</id>
        <url>https://oss.sonatype.org/content/groups/public</url>
    </repository>
</repositories>
```

# How to do Doctest

For a simple showcase project take a look at https://github.com/mavenplugins/maven-doctest-plugin/tree/master/doctesting.

A sample report would look like this: http://mavenplugins.github.com/maven-doctest-plugin/doctesting/doctests/index.html

## The basics

```java
import com.github.mavenplugins.doctest.AbstractRequestData;
...
@RunWith(DoctestRunner.class)
public class ShowcaseDoctest {

    class MyEndpoint extends AbstractRequestData {
        
        public URI getURI() throws URISyntaxException {
            return new URI("http://localhost:12345/my/endpoint");
        }
        
    }
    
    /**
     * This request should get a valid result!
     */
    @Doctest(value = MyEndpoint.class)
    public void myDoctest(HttpResponse response) throws Exception {
    }

}
```

You can also use ``@SimpleDoctest`` since Version 1.1.0:

```java
@RunWith(DoctestRunner.class)
public class ShowcaseDoctest {

    /**
     * This request should get a valid result!
     */
    @SimpleDoctest("http://localhost:12345/my/endpoint")
    public void myDoctest(HttpResponse response) throws Exception {
    }

}
```

... assuming your Server is up and running during the test cases.
The maven configuration looks like:

```xml
<project ...>
    ...
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <executions>
                    <execution>
                        <configuration>
                            <includes>
                                <include>**/*Doctest.java</include>
                            </includes>
                        </configuration>
                        <goals>
                            <goal>test</goal>
                        </goals>
                        <phase>integration-test</phase>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>com.github.mavenplugins.maven-doctest-plugin</groupId>
                <artifactId>doctest-plugin</artifactId>
                <version>1.8.1</version>
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.10</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare</goal>
                        </goals>
                        <phase>pre-integration-test</phase>
                    </execution>
                </executions>
            </plugin>
            ...
        </plugins>
    </build>
    
    <dependencies>
        <dependency>
            <groupId>com.github.mavenplugins.maven-doctest-plugin</groupId>
            <artifactId>doctest</artifactId>
            <version>1.8.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.10</version>
            <scope>test</scope>
        </dependency>
        ...
    </dependencies>
    
    <reporting>
        <plugins>
            <plugin>
                <groupId>com.github.mavenplugins.maven-doctest-plugin</groupId>
                <artifactId>doctest-plugin</artifactId>
                <version>1.8.1</version>
            </plugin>
        </plugins>
    </reporting>
    
</project>
```

To get the doctest report type ``mvn clean install site``.

## specify an order for your doctests

With the ``@DoctestOrder`` annotation (since version 1.1.0) you can now determine the order in which the requests are performed:

```java
RunWith(DoctestRunner.class)
public class ShowcaseDoctest {

    @SimpleDoctest("http://localhost:12345/_prepareTest")
    @DoctestOrder(Integer.MIN_VALUE)
    public void prepare(HttpResponse response) throws Exception {
    }
    
    @SimpleDoctest("http://localhost:12345/_printDebugInfo")
    @DoctestOrder(Integer.MIN_VALUE)
    public void printInitialState(HttpResponse response) throws Exception {
    }
    
    @SimpleDoctest("http://localhost:12345/my/endpoint")
    public void myEndpoint(HttpResponse response) throws Exception {
    }
    
    @SimpleDoctest("http://localhost:12345/_printDebugInfo")
    @DoctestOrder(Integer.MAX_VALUE)
    public void printState(HttpResponse response) throws Exception {
    }
    
    @SimpleDoctest("http://localhost:12345/_cleanDB")
    @DoctestOrder(Integer.MAX_VALUE)
    public void cleanDB(HttpResponse response) throws Exception {
    }

}
```

The example above initializes the DB, prints some information before the test, doing the actual test and clean up the DB after the test.
If no ``@DoctestOrder`` annotation could be found a value of ``0`` is assumed.

## controlling the http client

It's sometimes useful to have control over the way the http client handle redirects or other things.
You can archive this on a per-testcase basis using the ``@DoctestClient`` annotation:

```java
RunWith(DoctestRunner.class)
public class ShowcaseDoctest {

    @SimpleDoctest("http://localhost:12345/my/endpoint")
    @DoctestClient(handleRedirects = false)
    public void testCorrectRedirection(HttpResponse response) throws Exception {
    }

}
```

The example above demonstrates how to disable automatic redirect following.
This is especially useful when redirecting the client is the result of an endpoint.

Options:

* handleRedirects
* rejectRelativeRedirects
* allowCircularRedirects
* maxRedirects
* enableCompression

## setting default values

Since version 1.6.0 of the doctest library one has the ability to set default values
for the host, port, protocol (scheme) and the context path in which the application runs.

```java
RunWith(DoctestRunner.class)
@DoctestHost(host = "localhost", port = 12345)
public class ShowcaseDoctest {

    @SimpleDoctest("/my/endpoint")
    public void test(HttpResponse response) {
        // request to http://localhost:12345/my/endpoint
    }

}
```

The ``@DoctestHost`` annotation is used to configure the connection defaults for a test-class.
It can be applied at class-level or method-level with the following constraints:

* class-level appliances mean a general configuration for all doctests in that test-class
* method-level annotation override particular values (or all) of the class-level annotation
* URI's returned from the ``RequestData.getURI()`` method will be override both class and method-level values (``com.github.mavenplugins.doctest.AbstractRequestData.getURI()`` returns ``null`` be default)

```java
RunWith(DoctestRunner.class)
@DoctestHost(host = "localhost", port = 12345)
public class ShowcaseDoctest {

    @SimpleDoctest("/my/endpoint")
    public void test1(HttpResponse response) {
        // request to http://localhost:12345/my/endpoint
    }

    @SimpleDoctest("/endpoint")
    @DoctestHost(contextPath = "/my/")
    public void test2(HttpResponse response) {
        // request to http://localhost:12345/my/endpoint
    }
    
    @SimpleDoctest("/a/whole/other/service")
    @DoctestHost(port = 8080)
    public void test3(HttpResponse response) {
        // request to http://localhost:8080/a/whole/other/service
    }

}
```

## storing response-data between requests

Since every test-method runs stateless it's a bit tricky to share data between requests.
The doctest-library is able to store cookies, headers and response-entities between requests
with the ``DoctestStore`` and ``DoctestStores`` annotations.

All value are stored in a ``java.util.Map`` each for every Doctest-Class.
The ``id`` attribute of ``DoctestStore`` acts as key.
The values can be used via the ``Java Unified Expression Language`` (JUEL - http://juel.sourceforge.net/index.html)
in URL and / or path strings.

Cookies are stored as ``org.apache.http.cookie.Cookie`` and Headers are stored as ``org.apache.http.Header``.
The response-entities are stored as is.

```java
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
    
}
```

Once a variable is stored it can be overridden by using the same store-id within another request.

## stress-testing an endpoint

You also have the possibility to perform a stress-test on a specific endpoint (since version 1.1.0):

```java
RunWith(DoctestRunner.class)
public class ShowcaseDoctest {

    @SimpleDoctest("http://localhost:12345/my/endpoint")
    @DoctestConfig(maxConcurrentRequests = 32, requestCount = 1024, requestDelay = 1)
    public void testCorrectConcurrencyHandling(HttpResponse response) throws Exception {
    }

}
```

The test above performs 1024 requests with 32 connections (and threads)
the request delay determines the delay between each request per thread in milliseconds.

Another option of the ``@DoctestConfig`` annotation is ``assertionMode``, which determines which response is passed to the test method.
By default the assertionMode is set to LAST, which means that only the last response will be passed to the test-method.

The test-method itself is only called once, but the ``@Expect...`` annotations are check for every response.

If you don't specify a ``@DoctestConfig`` annotation, ``1`` request with ``1`` connection and no delay is applied.

## expecting a special response

There are two more annotations for doctests:

* @ExpectStatus - for the status code of a response
* @ExpectHeaders - for the response headers

Example:

```java
/**
 * My documentation for this doctest.
 */
@Doctest(value = EndpointAddress.class)
@ExpectStatus(200)
@ExpectHeaders({ @ExpectHeader(name = "Content-Type", content = "application/json.*") })
public void myJsonTest(HttpResponse response, JsonNode document) throws Exception {
}
```

## Cookie-Sharing between requests

Since version 1.5.0 it's possible to define custom cookie scopes between different request within the one Test-Class.
Before that every request had it's own cookie-store - which means that a cookie set by the server-response were totally
ignored by the following request.

Now you can assign a cookie-store to more than one request - which is pretty useful for cookie-session handling.

The ``DoctestCookieConfig`` annotation allows you to define (at method and class level) how to store cookies for the
next request.

```java
RunWith(DoctestRunner.class)
@DoctestCookieConfig(name = "default", store = Store.NEW)
public class ShowcaseDoctest {

    @SimpleDoctest("http://localhost:12345/endpoint/which/sets/cookies")
    @DoctestOrder(1)
    public void getSomeCookies(HttpResponse response) {
        ...
    }
    
    @SimpleDoctest("http://localhost:12345/endpoint/which/needs/cookies")
    @DoctestOrder(2)
    public void useSomeCookies(HttpResponse response) {
        // fails due to the NEW option in the cookie-store config ...
    }

}
```

The standard behaviour is to store cookie from all responses and send them with all requests.
But as mentioned above you can define scopes which methods should share cookies and which not.
The scoping of cookies will allow you to handle multiple sessions in one Test-Case easily.

```java
RunWith(DoctestRunner.class)
public class ShowcaseDoctest {

    /**
     * The cookies set here are not available for other requests.
     */
    @SimpleDoctest("http://localhost:12345/endpoint/which/sets/cookies")
    @DoctestOrder(1)
    @DoctestCookieConfig(name = "customScope", store = Store.NEW)
    public void getSomeCookies(HttpResponse response) {
        ...
    }
    
    /**
     * Test-Class wide availability of cookies.
     */
    @SimpleDoctest("http://localhost:12345/endpoint/which/sets/cookies")
    @DoctestOrder(1)
    public void getSomeCookiesForLaterUse(HttpResponse response) {
        ...
    }
    
    @SimpleDoctest("http://localhost:12345/endpoint/which/needs/cookies")
    @DoctestOrder(2)
    public void useSomeCookies(HttpResponse response) {
        // succeed, because the cookies from getSomeCookiesForLaterUse are still available
    }
    
    /**
     * The cookie from here are only available for test-methods with the cookie-scope "specialScope".
     */
    @SimpleDoctest("http://localhost:12345/special/endpoint/which/sets/cookies")
    @DoctestOrder(1)
    @DoctestCookieConfig(name = "specialScope", store = Store.SHARE)
    public void getSpecialCookies(HttpResponse response) {
        ...
    }
    
    @SimpleDoctest("http://localhost:12345/special/endpoint/which/needs/cookies")
    @DoctestOrder(2)
    @DoctestCookieConfig(name = "specialScope", store = Store.SHARE)
    public void useSpecialCookies(HttpResponse response) {
        // succeed, because the cookies from getSpecialCookies are only available here
    }

}
```

## doctest method signatures

Currently are only these doctest method signatures allowed:

* public void ``name``(org.apache.http.HttpResponse[, com.github.mavenplugins.doctest.DoctestContext])
* public void ``name``(org.apache.http.HttpResponse[, com.github.mavenplugins.doctest.DoctestContext], com.fasterxml.jackson.databind.JsonNode)
* public void ``name``(org.apache.http.HttpResponse[, com.github.mavenplugins.doctest.DoctestContext], org.w3c.dom.Document)
* public void ``name``(org.apache.http.HttpResponse[, com.github.mavenplugins.doctest.DoctestContext], byte[])
* public void ``name``(org.apache.http.HttpResponse[, com.github.mavenplugins.doctest.DoctestContext], java.lang.String)
* public void ``name``(org.apache.http.HttpResponse[, com.github.mavenplugins.doctest.DoctestContext], *any java bean that is automatically deserialized*)

A doctest-method also have to be annotated with ``@Doctest`` (or ``@SimpleDoctest``).
Methods annotated with ``@Test`` are entirely ignored.
The test class have to be annotated ``@RunWith(DoctestRunner.class)``.

The ``com.github.mavenplugins.doctest.DoctestContext`` parameter is optional and is interchangeable with the payload parameter.

## formating responses for the report

You have the ability to define an ``com.github.mavenplugins.doctest.formatter.EntityFormatter`` for formatting the response in the resulting report.
By default there are ``com.github.mavenplugins.doctest.formatter.XmlPrettyPrinter`` and ``com.github.mavenplugins.doctest.formatter.JsonPrettyPrinter``
for XML and JSON, but you can write your own one.

## validating responses

The doctest lib came with some assertion utilities useful when working with both raw responses and parsed objects (JsonNode and Document).

* com.github.mavenplugins.doctest.asserts.HttpResponseAssertUtils
* com.github.mavenplugins.doctest.asserts.JsonAssertUtils
* com.github.mavenplugins.doctest.asserts.XmlAssertUtils

``HttpResponseAssertUtils`` contains method to checks headers and so on.

``XmlAssertUtils`` uses XPath expressions to count elements and therefore enable a quick one line verification of the response got back from you endpoint.

``JsonAssertUtils``, too, uses XPath expressions to count nodes.

Examples:

```json
{
	"node1": {
		"name": "node1",
		"node1-1": {
			"name": "node1.1"
		}
	}
}
```

```java
JsonAssertUtils.assertExists("should be true", node, "node1/node1-1[@name='node1.1']");
JsonAssertUtils.assertExists("should be true, too", node, "//node1-1[@name='node1.1']");
JsonAssertUtils.assertExists("should fail", node, "//*[@name='node1.2']");
```

If you are already familiar with XPath you see the powerful, easy to use response verification ...

Another way (since version 1.5.0) would be to let the doctest lib deserialize the response entity for you:

```java
RunWith(DoctestRunner.class)
public class ShowcaseDoctest {

    class User {
    
        protected String username;
        protected String id;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
    }

    @SimpleDoctest("http://localhost:12345/my/xml/endpoint")
    public void gettingXMLDocument(HttpResponse response, Document document) {
        ...
    }

    @SimpleDoctest("http://localhost:12345/my/xml/endpoint")
    public void gettingObjectsDirectlyXML(HttpResponse response, User user) {
        assertEquals("mike", user.getUsername());
    }

    @SimpleDoctest("http://localhost:12345/my/json/endpoint")
    public void gettingJsonNode(HttpResponse response, JsonNode node) {
        ...
    }

    @SimpleDoctest("http://localhost:12345/my/json/endpoint")
    public void gettingObjectsDirectlyJSON(HttpResponse response, User user) {
        assertEquals("mike", user.getUsername());
    }

}
```

## Sending Data

### Form Data

```java
class Upload extends AbstractRequestData {
    
    @Override
    public URI getURI() throws URISyntaxException {
        return ...;
    }
    
    @Override
    public String getMethod() {
        return HttpPost.METHOD_NAME;
    }
    
    @Override
    public HttpEntity getHttpEntity() {
        MultipartEntity m = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        
        try {
            m.addPart("name", new StringBody("title", "text/plain", Charset.forName("UTF-8")));
            m.addPart("file", new FileBody(new File("./src/test/resources/images/test.gif"), "image/gif"));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        
        return m;
    }
    
}
```

### JSON Data

```java
class Upload extends AbstractRequestData {
    
    @Override
    public URI getURI() throws URISyntaxException {
        return ...;
    }
    
    @Override
    public String getMethod() {
        return HttpPut.METHOD_NAME;
    }
    
    @Override
    public HttpEntity getHttpEntity() {
        UserObject user = new UserObject();
        
        user.setName("name");
        user.setAge(34);
        ...
        
        return getJsonHttpEntity(user);
    }
    
}
```

### Custom Data

```java
class Upload extends AbstractRequestData {
    
    @Override
    public URI getURI() throws URISyntaxException {
        return ...;
    }
    
    @Override
    public String getMethod() {
        return HttpPut.METHOD_NAME;
    }
    
    @Override
    public HttpEntity getHttpEntity() {
        return new ByteArrayEntity("{\"name\":\"Jack\",\"age\":34}".getBytes(), ContentType.APPLICATION_JSON);
    }
    
}
```


## Examples

### File upload

```java
@RunWith(DoctestRunner.class)
public class UploadDoctest {
    
    class Upload extends AbstractRequestData {
        
        @Override
        public URI getURI() throws URISyntaxException {
            return new URI("http://localhost:12345/upload");
        }
        
        @Override
        public String getMethod() {
            return HttpPost.METHOD_NAME;
        }
        
        @Override
        public HttpEntity getHttpEntity() {
            MultipartEntity m = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            
            try {
                m.addPart("name", new StringBody("title", "text/plain", Charset.forName("UTF-8")));
                m.addPart("file", new FileBody(new File("./src/test/resources/images/test.gif"), "image/gif"));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            
            return m;
        }
        
    }
    
     /**
      * returns a JSON object like:
      * {
      *     'name': 'filename',
      *     'size': 1234567,
      *     'contentType': 'image/gif'
      * }
      */
    @Doctest(Upload.class)
    @DoctestOrder(1)
    @ExpectStatus(201)
    @ExpectHeaders({ @ExpectHeader(name = "Content-Type", content = "application/json.*") })
    public void upload(HttpResponse response, JsonNode node) throws Exception {
        assertTrue(node.isObject());
        JsonAssertUtils.assertExists(node, "/name['title']");
        JsonAssertUtils.assertExists(node, "/size['668418']");
        JsonAssertUtils.assertExists(node, "/contentType['image/gif']");
    }
    
    /**
     * Receives the binary file.
     */
    @SimpleDoctest("http://localhost:12345/download/file1")
    @DoctestOrder(2)
    @ExpectStatus(200)
    @ExpectHeaders({ @ExpectHeader(name = "Content-Type", content = "image/gif") })
    public void download(HttpResponse response) throws Exception {
        assertTrue(
            Arrays.equals(
                FileUtils.readFileToByteArray(new File("./src/test/resources/images/test.gif")),
                EntityUtils.toByteArray(response.getEntity())
            )
        );
    }
    
}
```
