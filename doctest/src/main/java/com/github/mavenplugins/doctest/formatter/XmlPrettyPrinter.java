package com.github.mavenplugins.doctest.formatter;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.HttpEntity;

/**
 * Formats the given xml data using indentation.
 */
public class XmlPrettyPrinter implements EntityFormatter {
    
    /**
     * The standard XML formatter.
     */
    protected Transformer transformer;
    
    /**
     * Sets up the transformer.
     */
    public XmlPrettyPrinter() throws TransformerConfigurationException, TransformerFactoryConfigurationError {
        transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    }
    
    /**
     * Returns an empty string if the response data is empty.
     */
    public String format(HttpEntity entity, byte[] responseData) throws Exception {
        StringWriter writer = new StringWriter();
        if (responseData != null && responseData.length > 0) {
            transformer.transform(new StreamSource(new ByteArrayInputStream(responseData)), new StreamResult(writer));
        }
        return writer.toString();
    }
    
}
