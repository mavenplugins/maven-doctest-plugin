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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
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
    
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Pattern JAVADOC_STAR_FINDER = Pattern.compile("^\\s*\\*\\s?", Pattern.MULTILINE);
    private static final Pattern JAVADOC_EMPTYLINE_FINDER = Pattern.compile("^\\s*\\*\\s*$", Pattern.MULTILINE);
    private static final Pattern ANY_METHOD_FINDER = Pattern.compile(
            "public\\s+void\\s+.*\\s*\\((HttpResponse|" + HttpResponse.class.getName().replaceAll("\\.", "\\\\.") + ")", Pattern.CASE_INSENSITIVE
                    | Pattern.MULTILINE);
    private static final SinkEventAttributeSet TABLE_CELL_STYLE_ATTRIBUTES = new SinkEventAttributeSet(new String[] { "style", "width:150px;" });
    private static final String JAVASCRIPT_CODE = "<script type=\"text/javascript\">function toggleVisibility(t){var e=document.getElementById(t);if(e.style.display=='block'){e.style.display='none';}else{e.style.display='block';}}</script>";
    
    /**
     * A container which encapsulates endpoints and contains the corresponding doctests.
     */
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
    
    /**
     * A container for the doctest data (request, response, javadoc).
     */
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
    
    /**
     * The number of characters that can be seen without hitting the "more details" button.
     * 
     * @parameter expression="${project.reporting.doctests.maxPreview}" default-value="128"
     */
    private int maxPreview = 128;
    
    /**
     * the java back-store which has the information where the result are situated.
     */
    protected Preferences prefs = Preferences.userNodeForPackage(DoctestRunner.class);
    /**
     * the report is sorted by endpoint, this map holds them.
     */
    protected Map<String, DoctestsContainer> endpoints = new TreeMap<String, DoctestsContainer>();
    /**
     * the json mapper used to read the doctest results.
     */
    protected ObjectMapper mapper = new ObjectMapper();
    
    public int getMaxPreview() {
        return maxPreview;
    }
    
    public void setMaxPreview(int maxPreview) {
        this.maxPreview = maxPreview;
    }
    
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
    
    /**
     * Parses and renders the doctest results using {@link #parseDoctestResults(File, String)} and {@link #renderDoctestResults(Locale)}.
     */
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
    
    /**
     * renders the basic scaffolding via the {@link Sink}. the actual report rendering is done via {@link #renderReport(Sink, Locale)}.
     */
    protected void renderDoctestResults(Locale locale) throws RendererException {
        Sink sink = getSink();
        
        sink.head();
        sink.title();
        sink.text(getBundle(locale).getString("header.title"));
        sink.title_();
        sink.head_();
        
        sink.body();
        sink.rawText(JAVASCRIPT_CODE);
        renderReport(sink, locale);
        sink.body_();
        
        sink.flush();
    }
    
    /**
     * Iterates through all enpoints and renders all doctest method for each endpoint.
     */
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
                
                if (!StringUtils.isEmpty(doctest.getValue().getJavaDoc())) {
                    sink.verbatim(SinkEventAttributeSet.BOXED);
                    sink.rawText(doctest.getValue().getJavaDoc());
                    sink.verbatim_();
                }
                
                sink.table();
                
                sink.tableRow();
                sink.tableCell(TABLE_CELL_STYLE_ATTRIBUTES);
                sink.bold();
                sink.text(requestLabel);
                sink.bold_();
                sink.tableCell_();
                sink.tableCell();
                renderRequestCell(sink, doctest.getValue().getRequest(), counter, detailLabel);
                sink.tableCell_();
                sink.tableRow_();
                
                sink.tableRow();
                sink.tableCell(TABLE_CELL_STYLE_ATTRIBUTES);
                sink.bold();
                sink.text(responseLabel);
                sink.bold_();
                sink.tableCell_();
                sink.tableCell();
                renderResponseCell(sink, doctest.getValue().getResponse(), counter, detailLabel);
                sink.tableCell_();
                sink.tableRow_();
                
                sink.table_();
                sink.section3_();
            }
            sink.section2_();
        }
        sink.section1_();
    }
    
    /**
     * Renders the request cell in the table
     */
    protected void renderRequestCell(Sink sink, RequestResultWrapper wrapper, AtomicInteger counter, String details) {
        StringBuilder builder = new StringBuilder();
        String preview;
        int id = counter.incrementAndGet();
        
        builder.append(wrapper.getRequestLine());
        builder.append("<br/>");
        builder.append("<a href=\"javascript:\" onclick=\"toggleVisibility('request-detail-");
        builder.append(id);
        builder.append("');toggleVisibility('request-detail-");
        builder.append(id);
        builder.append("-preview');\">");
        builder.append(details);
        builder.append("</a><br/><div id=\"request-detail-");
        builder.append(id);
        builder.append("-preview\" style=\"display: block;\">");
        
        sink.rawText(builder.toString());
        builder.delete(0, builder.length());
        
        preview = wrapper.getEntity();
        if (!StringUtils.isEmpty(wrapper.getEntity()) && wrapper.getEntity().length() <= maxPreview) {
            preview = wrapper.getEntity();
        } else if (!StringUtils.isEmpty(wrapper.getEntity())) {
            preview = wrapper.getEntity().substring(0, maxPreview) + "&hellip;";
        }
        
        if (!StringUtils.isEmpty(wrapper.getEntity())) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            sink.rawText(preview);
            sink.verbatim_();
        }
        
        builder.append("</div>");
        builder.append("<div id=\"request-detail-");
        builder.append(id);
        builder.append("\" style=\"display: none;\">");
        
        sink.rawText(builder.toString());
        builder.delete(0, builder.length());
        
        if (wrapper.getHeader() != null && wrapper.getHeader().length > 0) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            for (String header : wrapper.getHeader()) {
                sink.rawText(header);
                sink.rawText("<br/>");
            }
            sink.verbatim_();
        }
        if (wrapper.getParemeters() != null && wrapper.getParemeters().length > 0) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            for (String parameter : wrapper.getParemeters()) {
                sink.rawText(parameter);
                sink.rawText("<br/>");
            }
            sink.verbatim_();
        }
        if (!StringUtils.isEmpty(wrapper.getEntity())) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            sink.rawText(wrapper.getEntity());
            sink.verbatim_();
        }
        sink.rawText("</div>");
    }
    
    /**
     * Renders the response cell in the table.
     */
    protected void renderResponseCell(Sink sink, ResponseResultWrapper wrapper, AtomicInteger counter, String details) {
        StringBuilder builder = new StringBuilder();
        String preview;
        int id = counter.incrementAndGet();
        
        builder.append(wrapper.getStatusLine());
        builder.append("<br/>");
        builder.append("<a href=\"javascript:\" onclick=\"toggleVisibility('response-detail-");
        builder.append(id);
        builder.append("');toggleVisibility('response-detail-");
        builder.append(id);
        builder.append("-preview');\">");
        builder.append(details);
        builder.append("</a><br/><div id=\"response-detail-");
        builder.append(id);
        builder.append("-preview\" style=\"display: block;\">");
        
        sink.rawText(builder.toString());
        builder.delete(0, builder.length());
        
        preview = wrapper.getEntity();
        if (!StringUtils.isEmpty(wrapper.getEntity()) && wrapper.getEntity().length() <= maxPreview) {
            preview = wrapper.getEntity();
        } else if (!StringUtils.isEmpty(wrapper.getEntity())) {
            preview = wrapper.getEntity().substring(0, maxPreview) + "&hellip;";
        }
        
        if (!StringUtils.isEmpty(wrapper.getEntity())) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            sink.rawText(preview);
            sink.verbatim_();
        }
        
        builder.append("</div>");
        builder.append("<div id=\"response-detail-");
        builder.append(id);
        builder.append("\" style=\"display: none;\">");
        
        sink.rawText(builder.toString());
        builder.delete(0, builder.length());
        
        if (wrapper.getHeader() != null && wrapper.getHeader().length > 0) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            for (String header : wrapper.getHeader()) {
                sink.rawText(header);
                sink.rawText("<br/>");
            }
            sink.verbatim_();
        }
        if (wrapper.getParemeters() != null && wrapper.getParemeters().length > 0) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            for (String parameter : wrapper.getParemeters()) {
                sink.rawText(parameter);
                sink.rawText("<br/>");
            }
            sink.verbatim_();
        }
        if (!StringUtils.isEmpty(wrapper.getEntity())) {
            sink.verbatim(SinkEventAttributeSet.BOXED);
            sink.rawText(wrapper.getEntity());
            sink.verbatim_();
        }
        sink.rawText("</div>");
    }
    
    /**
     * Gets the doctest results and transforms them into {@link DoctestsContainer} objects.
     */
    protected void parseDoctestResults(File doctestResultDirectory, String doctestResultDirectoryName) {
        String tmp;
        String className;
        String doctestName;
        String source;
        DoctestsContainer endpoint;
        DoctestData doctest;
        RequestResultWrapper requestResult;
        ResponseResultWrapper responseResult;
        
        for (File resultFile : doctestResultDirectory.listFiles()) {
            tmp = resultFile.getAbsolutePath();
            if (tmp.endsWith(".request")) {
                tmp = tmp.substring(0, tmp.lastIndexOf('.'));
                className = tmp.substring(doctestResultDirectoryName.length(), tmp.indexOf('-', doctestResultDirectoryName.length()));
                className = className.replaceAll("\\.", "/") + ".java";
                doctestName = tmp.substring(tmp.indexOf('-', doctestResultDirectoryName.length()) + 1);
                
                try {
                    requestResult = mapper.readValue(new File(tmp + ".request"), RequestResultWrapper.class);
                    responseResult = mapper.readValue(new File(tmp + ".response"), ResponseResultWrapper.class);
                    source = FileUtils.readFileToString(new File(project.getBuild().getTestSourceDirectory(), className));
                    
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
                    
                    doctest = new DoctestData();
                    doctest.setJavaDoc(getJavaDoc(source, doctestName));
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
    
    /**
     * Gets the javadoc comment situated over a doctest method.
     */
    protected String getJavaDoc(String source, String method) {
        Pattern methodPattern = Pattern.compile(
                "public\\s+void\\s+" + method + "\\s*\\((HttpResponse|" + HttpResponse.class.getName().replaceAll("\\.", "\\\\.") + ")",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = methodPattern.matcher(source);
        int start, tmp, last, comment;
        String doc;
        
        if (matcher.find()) {
            start = matcher.start();
            last = -1;
            matcher = ANY_METHOD_FINDER.matcher(source);
            while (matcher.find() && (tmp = matcher.start()) < start) {
                last = tmp;
            }
            
            comment = source.lastIndexOf("/**", start);
            
            if (comment > 2 && (comment > last || last == -1)) {
                doc = source.substring(comment, source.indexOf("*/", comment));
                doc = doc.substring(3, doc.length() - 2);
                doc = JAVADOC_EMPTYLINE_FINDER.matcher(doc).replaceAll(LINE_SEPARATOR);
                doc = JAVADOC_STAR_FINDER.matcher(doc).replaceAll("");
                doc = StringUtils.replace(doc, " ", "&nbsp;");
                doc = StringUtils.replace(doc, LINE_SEPARATOR, "<br/>");
                return doc;
            }
        }
        
        return "";
    }
    
    /**
     * Escapes an array of strings.
     */
    protected String[] escapeToHtml(String[] texts) {
        for (int i = 0; i < texts.length; i++) {
            texts[i] = escapeToHtml(texts[i]);
        }
        return texts;
    }
    
    /**
     * Escapes a single string.
     */
    protected String escapeToHtml(String text) {
        return StringUtils.replace(StringUtils.replace(HtmlTools.escapeHTML(text, false), "&amp;#", "&#"), LINE_SEPARATOR, "<br/>");
    }
    
}
