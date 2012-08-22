# Maven Doctest Plugin - Python-like Doctesting for Java
http://mavenplugins.github.com/maven-doctest-plugin/

Writing tests for you REST API that also documents it by example is a great asset in the python world.
This project tries to enable java developers to benefit from the same testing comfort and documentation mechanism.

# License

* Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

# Requirements / Dependencies

* Java 1.5+ (http://www.java.com/de/download/)
* JUnit 4+ (http://www.junit.org/)
* Jackson 2.0.2+ (https://github.com/FasterXML/jackson-core/)

# How to get it

The maven dependency for doctesting::

```xml
<dependency>
    <groupId>com.github.mavenplugins.maven-doctest-plugin</groupId>
    <artifactId>doctest</artifactId>
    <version>1.1.0</version>
    <scope>test</scope>
</dependency>
```

The maven reporting-plugin::

```xml
<reporting>
    <plugins>
    	...
        <plugin>
            <groupId>com.github.mavenplugins.maven-doctest-plugin</groupId>
            <artifactId>doctest-plugin</artifactId>
            <version>1.1.0</version>
        </plugin>
        ...
    </plugins>
</reporting>
```

And the corresponding repository::

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
                <version>1.1.0</version>
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
            <version>1.1.0</version>
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
                <version>1.1.0</version>
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
    public void prepare(HttpResponse response) throws Exception {
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

## stress-testing an endpoint

You also have the possibility to perform a stress-test on an specific endpoint (since version 1.1.0):

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

## doctest method signatures

Currently are only these doctest method signatures allowed:

* public void ``name``(org.apache.http.HttpResponse)
* public void ``name``(org.apache.http.HttpResponse, com.fasterxml.jackson.databind.JsonNode)
* public void ``name``(org.apache.http.HttpResponse, org.w3c.dom.Document)

A doctest-methods also have to be annotated with ``@Doctest``. Methods annotated with ``@Test`` are entirely ignored.
The test class have to be annotated ``@RunWith(DoctestRunner.class)``.

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
