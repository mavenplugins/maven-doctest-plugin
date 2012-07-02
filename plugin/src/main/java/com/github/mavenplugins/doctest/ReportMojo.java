/**
 * Copyright 2012 the contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.github.mavenplugins.doctest;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributeSet;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.util.HtmlTools;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * This Mojo reports the doctest results.
 * 
 * @goal report
 * @phase site
 */
public class ReportMojo extends AbstractMavenReport {
    
    private static final SinkEventAttributeSet TABLE_CELL_STYLE_ATTRIBUTES = new SinkEventAttributeSet(new String[] { "style", "width:150px;" });
    
    private static final String JAVASCRIPT_CODE = "<script type=\"text/javascript\">function toggleVisibility(t){var e=document.getElementById(t);if(e.style.display=='block'){e.style.display='none';}else{e.style.display='block';}}</script>";
    private static final String CSS_CODE = "<style type=\"text/css\">.hiddenAtInitialization{display: none;}</style>";
    
    public class DoctestsContainer {
        
        protected Map<String, DoctestData> doctests = new TreeMap<String, DoctestData>();
        protected String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Map<String, DoctestData> getDoctests() {
            return doctests;
        }
        
        public void setDoctests(Map<String, DoctestData> doctests) {
            this.doctests = doctests;
        }
        
    }
    
    public class DoctestData {
        
        protected RequestResultWrapper request;
        protected ResponseResultWrapper response;
        protected String javaDoc = "";
        protected String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getJavaDoc() {
            return javaDoc;
        }
        
        public void setJavaDoc(String javaDoc) {
            this.javaDoc = javaDoc;
        }
        
        public RequestResultWrapper getRequest() {
            return request;
        }
        
        public void setRequest(RequestResultWrapper request) {
            this.request = request;
        }
        
        public ResponseResultWrapper getResponse() {
            return response;
        }
        
        public void setResponse(ResponseResultWrapper response) {
            this.response = response;
        }
        
    }
    
    /**
     * <i>Maven Internal</i>: The Doxia Site Renderer.
     * 
     * @component
     * @required
     * @readonly
     */
    private Renderer siteRenderer;
    
    /**
     * <i>Maven Internal</i>: The Project descriptor.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    
    protected Preferences prefs = Preferences.userNodeForPackage(DoctestRunner.class);
    protected Map<String, DoctestsContainer> endpoints = new TreeMap<String, DoctestsContainer>();
    protected ObjectMapper mapper = new ObjectMapper();
    
    @Override
    protected String getOutputDirectory() {
        return project.getReporting().getOutputDirectory();
    }
    
    @Override
    public Renderer getSiteRenderer() {
        return siteRenderer;
    }
    
    @Override
    protected MavenProject getProject() {
        return project;
    }
    
    public String getOutputName() {
        return "doctests/index";
    }
    
    public String getName(Locale locale) {
        return getBundle(locale).getString("name");
    }
    
    public String getDescription(Locale locale) {
        return getBundle(locale).getString("description");
    }
    
    protected ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("doctest", locale, this.getClass().getClassLoader());
    }
    
    public void setSiteRenderer(Renderer siteRenderer) {
        this.siteRenderer = siteRenderer;
    }
    
    public void setProject(MavenProject project) {
        this.project = project;
    }
    
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        File dir;
        File results = null;
        String doctestResults = "";
        
        try {
            prefs.sync();
            doctestResults = prefs.get(DoctestRunner.RESULT_PATH, "");
            results = new File(doctestResults);
            prefs.removeNode();
        } catch (BackingStoreException exception) {
            exception.printStackTrace();
        }
        
        if (!(dir = new File(project.getReporting().getOutputDirectory() + File.separator + "doctests")).exists()) {
            dir.mkdirs();
        }
        
        if (results != null && results.exists()) {
            parseDoctestResults(results, doctestResults);
            try {
                renderDoctestResults(locale);
            } catch (RendererException exception) {
                getLog().error("error while rendernig doctests.", exception);
            }
        }
    }
    
    protected void renderDoctestResults(Locale locale) throws RendererException {
        Sink sink = getSink();
        
        sink.head();
        sink.title();
        sink.text(getBundle(locale).getString("header.title"));
        sink.title_();
        sink.head_();
        
        sink.body();
        sink.rawText(JAVASCRIPT_CODE);
        sink.rawText(CSS_CODE);
        renderReport(sink, locale);
        sink.body_();
        
        sink.flush();
    }
    
    protected void renderReport(Sink sink, Locale locale) {
        AtomicInteger counter = new AtomicInteger();
        String requestLabel = escapeToHtml(getBundle(locale).getString("request.header"));
        String responseLabel = escapeToHtml(getBundle(locale).getString("response.header"));
        String detailLabel = escapeToHtml(getBundle(locale).getString("detail.label"));
        
        sink.section1();
        sink.sectionTitle1();
        sink.text(escapeToHtml(getBundle(locale).getString("toc.title")));
        sink.sectionTitle1_();
        
        sink.list();
        for (Map.Entry<String, DoctestsContainer> endpoint : endpoints.entrySet()) {
            sink.listItem();
            sink.anchor(endpoint.getKey());
            sink.text(endpoint.getKey());
            sink.anchor_();
            sink.listItem_();
        }
        sink.list_();
        
        for (Map.Entry<String, DoctestsContainer> endpoint : endpoints.entrySet()) {
            sink.section2();
            sink.sectionTitle2();
            sink.text(endpoint.getKey());
            sink.sectionTitle2_();
            
            for (Map.Entry<String, DoctestData> doctest : endpoint.getValue().getDoctests().entrySet()) {
                sink.section3();
                sink.sectionTitle3();
                sink.text(doctest.getKey());
                sink.sectionTitle3_();
                
                sink.paragraph();
                sink.rawText(doctest.getValue().getJavaDoc());
                sink.paragraph_();
                
                sink.table();
                
                sink.tableRow();
                sink.tableCell(TABLE_CELL_STYLE_ATTRIBUTES);
                sink.bold();
                sink.text(requestLabel);
                sink.bold_();
                sink.tableCell_();
                sink.tableCell();
                sink.rawText(renderRequestCell(doctest.getValue().getRequest(), counter, detailLabel));
                sink.tableCell_();
                sink.tableRow_();
                
                sink.tableRow();
                sink.tableCell(TABLE_CELL_STYLE_ATTRIBUTES);
                sink.bold();
                sink.text(responseLabel);
                sink.bold_();
                sink.tableCell_();
                sink.tableCell();
                sink.rawText(renderResponseCell(doctest.getValue().getResponse(), counter, detailLabel));
                sink.tableCell_();
                sink.tableRow_();
                
                sink.table_();
                sink.section3_();
            }
            sink.section2_();
        }
        sink.section1_();
    }
    
    protected String renderRequestCell(RequestResultWrapper wrapper, AtomicInteger counter, String details) {
        StringBuilder builder = new StringBuilder();
        int id = counter.incrementAndGet();
        
        builder.append(wrapper.getRequestLine());
        builder.append("<br/>");
        builder.append("<a href=\"javascript:\" onclick=\"toggleVisibility('request-detail-");
        builder.append(id);
        builder.append("');\">");
        builder.append(details);
        builder.append("</a>");
        builder.append("<div id=\"request-detail-");
        builder.append(id);
        builder.append("\" class=\"hiddenAtInitialization\">");
        if (wrapper.getHeader() != null && wrapper.getHeader().length > 0) {
            builder.append("<br/>");
            for (String header : wrapper.getHeader()) {
                builder.append(header);
                builder.append("<br/>");
            }
        }
        if (wrapper.getParemeters() != null && wrapper.getParemeters().length > 0) {
            builder.append("<br/>");
            for (String parameter : wrapper.getParemeters()) {
                builder.append(parameter);
                builder.append("<br/>");
            }
            
        }
        if (!StringUtils.isEmpty(wrapper.getEntity())) {
            builder.append("<br/>");
            builder.append(wrapper.getEntity());
        }
        builder.append("</div>");
        
        return builder.toString();
    }
    
    protected String renderResponseCell(ResponseResultWrapper wrapper, AtomicInteger counter, String details) {
        StringBuilder builder = new StringBuilder();
        int id = counter.incrementAndGet();
        
        builder.append(wrapper.getStatusLine());
        builder.append("<br/>");
        builder.append("<a href=\"javascript:\" onclick=\"toggleVisibility('response-detail-");
        builder.append(id);
        builder.append("');\">");
        builder.append(details);
        builder.append("</a>");
        builder.append("<div id=\"response-detail-");
        builder.append(id);
        builder.append("\" class=\"hiddenAtInitialization\">");
        if (wrapper.getHeader() != null && wrapper.getHeader().length > 0) {
            builder.append("<br/>");
            for (String header : wrapper.getHeader()) {
                builder.append(header);
                builder.append("<br/>");
            }
        }
        if (wrapper.getParemeters() != null && wrapper.getParemeters().length > 0) {
            builder.append("<br/>");
            for (String parameter : wrapper.getParemeters()) {
                builder.append(parameter);
                builder.append("<br/>");
            }
            
        }
        if (!StringUtils.isEmpty(wrapper.getEntity())) {
            builder.append("<br/>");
            builder.append(wrapper.getEntity());
        }
        builder.append("</div>");
        
        return builder.toString();
    }
    
    protected void parseDoctestResults(File doctestResultDirectory, String doctestResultDirectoryName) {
        String tmp;
        String className;
        String doctestName;
        DoctestsContainer endpoint;
        DoctestData doctest;
        RequestResultWrapper requestResult;
        ResponseResultWrapper responseResult;
        
        for (File resultFile : doctestResultDirectory.listFiles()) {
            tmp = resultFile.getAbsolutePath();
            if (tmp.endsWith(".request")) {
                tmp = tmp.substring(0, tmp.lastIndexOf('.'));
                className = tmp.substring(doctestResultDirectoryName.length(), tmp.indexOf('-', doctestResultDirectoryName.length()));
                doctestName = tmp.substring(tmp.indexOf('-', doctestResultDirectoryName.length()) + 1);
                
                try {
                    requestResult = mapper.readValue(new File(tmp + ".request"), RequestResultWrapper.class);
                    responseResult = mapper.readValue(new File(tmp + ".response"), ResponseResultWrapper.class);
                    
                    tmp = tmp.substring(doctestResultDirectoryName.length()).replace('-', '.');
                    endpoint = endpoints.get(requestResult.getPath());
                    if (endpoint == null) {
                        endpoint = new DoctestsContainer();
                        endpoint.setName(requestResult.getPath());
                        endpoints.put(requestResult.getPath(), endpoint);
                    }
                    
                    requestResult.setEntity(escapeToHtml(requestResult.getEntity()));
                    requestResult.setPath(escapeToHtml(requestResult.getPath()));
                    requestResult.setRequestLine(escapeToHtml(requestResult.getRequestLine()));
                    requestResult.setHeader(escapeToHtml(requestResult.getHeader()));
                    requestResult.setParemeters(escapeToHtml(requestResult.getParemeters()));
                    
                    responseResult.setEntity(escapeToHtml(responseResult.getEntity()));
                    responseResult.setStatusLine(escapeToHtml(responseResult.getStatusLine()));
                    responseResult.setHeader(escapeToHtml(responseResult.getHeader()));
                    responseResult.setParemeters(escapeToHtml(responseResult.getParemeters()));
                    
                    // TODO: get javadoc for the method
                    
                    doctest = new DoctestData();
                    doctest.setName(tmp);
                    doctest.setRequest(requestResult);
                    doctest.setResponse(responseResult);
                    endpoint.getDoctests().put(tmp, doctest);
                } catch (IOException exception) {
                    getLog().error("error while reading doctest request", exception);
                }
            }
        }
    }
    
    protected String[] escapeToHtml(String[] texts) {
        for (int i = 0; i < texts.length; i++) {
            texts[i] = escapeToHtml(texts[i]);
        }
        return texts;
    }
    
    protected String escapeToHtml(String text) {
        return StringUtils.replace(StringUtils.replace(HtmlTools.escapeHTML(text, false), "&amp;#", "&#"), "\n", "<br/>");
    }
    
}
