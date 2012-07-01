package com.github.mavenplugins.doctest.asserts;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

public class JsonAssertUtils {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public static void assertExists(JsonNode node, String xpath) throws Exception {
        assertExists(null, node, xpath);
    }
    
    public static void assertExists(String message, JsonNode node, String xpath) throws Exception {
        assertTrue(message, count(message, node, xpath) > 0);
    }
    
    public static int count(JsonNode node, String xpath) throws Exception {
        return count(null, node, xpath);
    }
    
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
