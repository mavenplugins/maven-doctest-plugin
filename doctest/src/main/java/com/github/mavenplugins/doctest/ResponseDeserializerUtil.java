package com.github.mavenplugins.doctest;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseDeserializerUtil {
    
    /**
     * XML factory.
     */
    private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();
    
    /**
     * A mapping with content-types to {@link ResponseDeserializer} objects.
     */
    protected Map<String, ResponseDeserializer> responseDeserializers = new HashMap<String, ResponseDeserializer>();
    /**
     * A mapping with content-types to classes.
     */
    protected Map<String, Class<?>> responseTypes = new HashMap<String, Class<?>>();
    /**
     * The json mapper.
     */
    protected ObjectMapper jsonMapper = new ObjectMapper();
    
    /**
     * Initiates the util with the specified test class.
     */
    public void init() {
        ResponseDeserializer jsonDeserializer = new ResponseDeserializer() {
            
            @Override
            public Object deserialize(byte[] data, Class<?> valueType) throws Exception {
                return jsonMapper.readValue(data, valueType);
            }
            
        };
        ResponseDeserializer xmlDeserializer = new ResponseDeserializer() {
            
            @Override
            public Object deserialize(byte[] data, Class<?> valueType) throws Exception {
                Unmarshaller unmarshaller = JAXBContext.newInstance(valueType).createUnmarshaller();
                return unmarshaller.unmarshal(new ByteArrayInputStream(data));
            }
            
        };
        
        responseDeserializers.put(ContentType.APPLICATION_JSON.getMimeType(), jsonDeserializer);
        responseDeserializers.put(ContentType.APPLICATION_XML.getMimeType(), xmlDeserializer);
        responseDeserializers.put(ContentType.APPLICATION_ATOM_XML.getMimeType(), xmlDeserializer);
        responseDeserializers.put(ContentType.APPLICATION_SVG_XML.getMimeType(), xmlDeserializer);
        responseDeserializers.put(ContentType.TEXT_XML.getMimeType(), xmlDeserializer);
        
        responseTypes.put(ContentType.APPLICATION_JSON.getMimeType(), Map.class);
    }
    
    /**
     * Parses the response and assigns the result as entry to the given context.
     */
    public void deserialize(ResponseContext ctx, Class<?> type) throws Exception {
        Object responseEntity = null;
        ResponseDeserializer responseDeserializer = null;
        HttpResponse response = ctx.getResponse();
        byte[] responseData = ctx.getResponseData();
        String contentType;
        
        if (type != null) {
            if (Document.class.isAssignableFrom(type)) {
                if (responseData == null || responseData.length == 0) {
                    responseEntity = null;
                } else {
                    responseEntity = FACTORY.newDocumentBuilder().parse(new ByteArrayInputStream(responseData));
                }
            } else if (JsonNode.class.isAssignableFrom(type) && response.getEntity() != null) {
                if (responseData == null || responseData.length == 0) {
                    responseEntity = null;
                } else {
                    responseEntity = jsonMapper.readTree(responseData);
                }
            } else if (byte[].class.isAssignableFrom(type)) {
                responseEntity = responseData;
            } else if (CharSequence.class.isAssignableFrom(type)) {
                responseEntity = new String(responseData, response.getEntity().getContentEncoding().getValue());
            } else {
                responseEntity = null;
                responseDeserializer = responseDeserializers.get(extractContentType(response));
                if (responseDeserializer != null) {
                    responseEntity = responseDeserializer.deserialize(responseData, type);
                }
            }
        } else {
            responseEntity = null;
            contentType = extractContentType(response);
            
            responseDeserializer = responseDeserializers.get(contentType);
            if (responseDeserializer != null) {
                if (responseTypes.containsKey(contentType)) {
                    responseEntity = responseDeserializer.deserialize(responseData, responseTypes.get(contentType));
                } else {
                    responseEntity = FACTORY.newDocumentBuilder().parse(new ByteArrayInputStream(responseData));
                }
            }
        }
        
        ctx.setEntity(responseEntity);
    }
    
    /**
     * Gets the response entity type the test-method expects.
     */
    public Class<?> getEntityType(Method method) {
        Class<?>[] methodParameters = method.getParameterTypes();
        
        if (methodParameters.length == 2) {
            if (!DoctestContext.class.isAssignableFrom(methodParameters[1])) {
                return methodParameters[1];
            }
        } else if (methodParameters.length == 3) {
            if (!DoctestContext.class.isAssignableFrom(methodParameters[1])) {
                return methodParameters[1];
            } else {
                return methodParameters[2];
            }
        }
        
        return null;
    }
    
    /**
     * Get the content-type of the response without any encoding information.
     */
    protected String extractContentType(HttpResponse response) {
        String contentType = "";
        int index = 0;
        Header[] header;
        
        if (response.getEntity() != null && response.getEntity().getContentType() != null) {
            contentType = response.getEntity().getContentType().getValue();
        } else if ((header = response.getHeaders(HttpHeaders.CONTENT_TYPE)) != null && header.length == 1) {
            contentType = header[0].getValue();
        }
        
        if (contentType != null && (index = contentType.indexOf(';')) != -1) {
            contentType = contentType.substring(0, index);
        }
        
        return contentType;
    }
    
}
