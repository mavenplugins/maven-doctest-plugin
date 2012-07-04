package com.github.mavenplugins.doctest.asserts;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

/**
 * This is an utility class used to check the elements of a given JSON node with XPath (using apache jxpath).
 */
public class JsonAssertUtils {
    
    /**
     * the object mapper (threadsafe)
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * Checks if the xpath expression results in at least 1 element.
     * 
     * @param node the json node.
     * @param xpath the expression
     */
    public static void assertExists(JsonNode node, String xpath) throws Exception {
        assertExists(null, node, xpath);
    }
    
    /**
     * Checks if the xpath expression results in at least 1 element.
     * 
     * @param node the json node.
     * @param xpath the expression
     * @param message the assert message
     */
    public static void assertExists(String message, JsonNode node, String xpath) throws Exception {
        assertTrue(message, count(message, node, xpath) > 0);
    }
    
    /**
     * Counts the elements which results from the xpath expression.
     * 
     * @param node the xml document.
     * @param xpath the expression
     * @return Returns -1 if no element was found.
     */
    public static int count(JsonNode node, String xpath) throws Exception {
        return count(null, node, xpath);
    }
    
    /**
     * Counts the elements which results from the xpath expression.
     * 
     * @param node the xml document.
     * @param xpath the expression
     * @param message the assert message
     * @return Returns -1 if no element was found.
     */
    @SuppressWarnings("unchecked")
    public static int count(String message, JsonNode node, String xpath) throws Exception {
        JXPathContext ctx = JXPathContext.newContext(MAPPER.readValue(new TreeTraversingParser(node), Map.class));
        int count = 0;
        Iterator<Object> iter = ctx.iterate(xpath);
        
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        
        return count;
    }
    
}
