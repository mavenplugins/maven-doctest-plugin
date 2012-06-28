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
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * This Mojo executes the actual doctests.
 * 
 * @goal report
 * @phase site
 */
public class ReportMojo extends AbstractMavenReport {
    
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
    
    protected Preferences prefs = Preferences.userNodeForPackage(AbstractDoctest.class);
    
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
        String doctestResults = "";
        
        try {
            prefs.sync();
            doctestResults = prefs.get(AbstractDoctest.RESULT_PATH, "");
            getLog().info("Doctest results are in " + doctestResults);
            prefs.removeNode();
        } catch (BackingStoreException exception) {
            exception.printStackTrace();
        }
        
        if (!(dir = new File(project.getReporting().getOutputDirectory() + File.separator + "doctests")).exists()) {
            dir.mkdirs();
        }
        
        // TODO
    }
    
}
