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

public class XmlAssertUtils {
    
    private static final Map<String, XPathExpression> EXPRESSIONS = new TreeMap<String, XPathExpression>();
    
    public static void assertExists(Document document, String xpath) throws XPathExpressionException {
        assertExists(null, document, xpath);
    }
    
    public static void assertExists(String message, Document document, String xpath) throws XPathExpressionException {
        assertTrue(message, count(message, document, xpath) > 0);
    }
    
    public static int count(Document document, String xpath) throws XPathExpressionException {
        return count(null, document, xpath);
    }
    
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
