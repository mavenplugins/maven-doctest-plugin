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

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * This Mojo prepares the path information for the doctests.
 * 
 * @goal prepare
 * @phase pre-integration-test
 */
public class PrepareMojo extends AbstractMojo {
    
    /**
     * <i>Maven Internal</i>: The Project descriptor.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    
    /**
     * The directory where the doctest result are written to.
     * 
     * @parameter expression="${project.reporting.doctestOutputDirectory}" default-value="${project.reporting.outputDirectory}/doctests/"
     */
    private String doctestResultPath;
    
    /**
     * The preference back store is used to transfer data between this maven plugin and the actual doctests, which doesn't know anything about the maven project
     * configuration or this plugin.
     */
    private Preferences prefs = Preferences.userNodeForPackage(AbstractDoctest.class);
    
    public String getDoctestResultPath() {
        return doctestResultPath;
    }
    
    public void setDoctestResultPath(String doctestResultPath) {
        this.doctestResultPath = doctestResultPath;
    }
    
    protected MavenProject getProject() {
        return project;
    }
    
    public void setProject(MavenProject project) {
        this.project = project;
    }
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            prefs.sync();
            prefs.put(AbstractDoctest.TEST_SOURCE_PATH, project.getBuild().getTestSourceDirectory());
            prefs.put(AbstractDoctest.RESULT_PATH, doctestResultPath);
        } catch (BackingStoreException exception) {
            getLog().error("error while setting test-class path to java back-store", exception);
        }
    }
    
}
