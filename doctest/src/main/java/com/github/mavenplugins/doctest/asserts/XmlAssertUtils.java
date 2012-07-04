package com.github.mavenplugins.doctest.asserts;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This is an utility class used to check the elements of a given XML document with XPath.
 */
public class XmlAssertUtils {
    
    /**
     * the expression cache.
     */
    private static final Map<String, XPathExpression> EXPRESSIONS = new TreeMap<String, XPathExpression>();
    
    /**
     * Checks if the xpath expression results in at least 1 element.
     * 
     * @param document the xml document.
     * @param xpath the expression
     */
    public static void assertExists(Document document, String xpath) throws XPathExpressionException {
        assertExists(null, document, xpath);
    }
    
    /**
     * Checks if the xpath expression results in at least 1 element.
     * 
     * @param document the xml document.
     * @param xpath the expression
     * @param message the assert message
     */
    public static void assertExists(String message, Document document, String xpath) throws XPathExpressionException {
        assertTrue(message, count(message, document, xpath) > 0);
    }
    
    /**
     * Counts the elements which results from the xpath expression.
     * 
     * @param document the xml document.
     * @param xpath the expression
     * @return Returns -1 if no element was found.
     */
    public static int count(Document document, String xpath) throws XPathExpressionException {
        return count(null, document, xpath);
    }
    
    /**
     * Counts the elements which results from the xpath expression.
     * 
     * @param document the xml document.
     * @param xpath the expression
     * @param message The assert message.
     * @return Returns -1 if no element was found.
     */
    public static int count(String message, Document document, String xpath) throws XPathExpressionException {
        XPathExpression expression = EXPRESSIONS.get(xpath);
        NodeList list = null;
        
        if (expression == null) {
            expression = XPathFactory.newInstance().newXPath().compile(xpath);
            EXPRESSIONS.put(xpath, expression);
        }
        
        if ((list = (NodeList) expression.evaluate(document.getDocumentElement(), XPathConstants.NODESET)) != null) {
            return list.getLength();
        }
        
        return -1;
    }
    
}
