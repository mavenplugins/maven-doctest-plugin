# Maven Doctest Plugin - Python-like Doctesting for Java
http://mavenplugins.github.com/maven-doctest-plugin/

Writing tests that also documents your API by example is a great asset in the python world.
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
    <version>1.0.0</version>
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
            <version>1.0.0</version>
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

For a simple showcase project take a look at ...
