/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.support.directives.procedures;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APIProcedure;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * This procedure is intended to ease Release process by:
 * <ol>
 * <li>changing the uAAL.pom parent version
 * <li>changing the imported root poms' versions in dependencyManagement
 * <li>changing the version of itest in dependencyManagement
 * <li>changing the version of uaal-maven-plugin in dependencyManagement
 * <li>changing the version of uaaldirectives-maven-plugin in
 * dependencyManagement
 * <li>changing the version of uaal-manifest-maven-plugin in
 * dependencyManagement
 * <li>the version of uaaldirectives-maven-plugin in reporting
 * </ol>
 * 
 * The versions are interactively resolved.
 * @author amedrano
 * 
 */
public class UpdateParentPomInteractiveProcedure implements APIProcedure,
	PomFixer {

    private static final String UAAL_GID = "org.universAAL";

    private static final String UAAL_AID = "uAAL.pom";

    public UpdateParentPomInteractiveProcedure() {
	super();
    }

    /** {@inheritDoc} */
    public void execute(MavenProject mavenProject, Log log)
	    throws MojoExecutionException, MojoFailureException {

	try {
	    new PomWriter(this, mavenProject).fix();
	} catch (Exception e) {
	    throw new MojoExecutionException("unable to fix Versions", e);
	}

    }

    public void fix(Model model) {
	// Update parent Version, if there is a parent (in case of uAAL.pom)
	if (model.getParent() != null
		&& model.getParent().getArtifactId().equals(UAAL_AID)
		&& model.getParent().getGroupId().equals(UAAL_GID)) {
	    model.getParent().setVersion(
		    ask4NewVersion(model.getParent().getGroupId(), model
			    .getParent().getArtifactId(), model.getParent()
			    .getVersion()));
	}
	// Update dependencyManagement
		if (model.getDependencyManagement() != null
				&& model.getDependencyManagement().getDependencies() != null) {
			List<Dependency> deps = model.getDependencyManagement()
					.getDependencies();
			List<Dependency> nld = new ArrayList<Dependency>();
			for (Dependency d : deps) {
				// Update imports
				if (d.getScope() != null && d.getScope().equals("import")) {
					d.setVersion(ask4NewVersion(d.getGroupId(),
							d.getArtifactId(), d.getVersion()));
				}
				// update support artifacts
				if (d.getGroupId().equals("org.universAAL.support")
						&& ((d.getArtifactId().equals("itests")
							|| d.getArtifactId().equals("uaal-maven-plugin")
							|| d.getArtifactId().equals("uaalDirectives-maven-plugin") 
							|| d.getArtifactId().equals("uaal-manifest-maven-plugin")))) {
					d.setVersion(ask4NewVersion(d.getGroupId(),
							d.getArtifactId(), d.getVersion()));
				}
				// update SCHEMAS
				if (d.getGroupId().equals("org.universAAL.middleware")
						&& d.getArtifactId().equals("mw.schemas")){
					d.setVersion(ask4NewVersion(d.getGroupId(),
							d.getArtifactId(), d.getVersion()));
				}
				nld.add(d);
			}
			model.getDependencyManagement().setDependencies(nld);
		}
	
	//update plugin management
	if (model.getBuild() != null
			&& model.getBuild().getPluginManagement() != null){
		List<Plugin> plugins = model.getBuild().getPluginManagement().getPlugins();
		List<Plugin> newPlugins = new ArrayList<Plugin>();
		for (Plugin p : plugins) {
			// update uAAL plugins
			if (p.getGroupId().equals("org.universAAL.support")
						&& ( p.getArtifactId().equals("uaal-maven-plugin")
							|| p.getArtifactId().equals("uaalDirectives-maven-plugin") 
							|| p.getArtifactId().equals("uaal-manifest-maven-plugin"))) {
			    p.setVersion(ask4NewVersion(p.getGroupId(),
					p.getArtifactId(), p.getVersion()));
			}
			newPlugins.add(p);
		}
		model.getBuild().getPluginManagement().setPlugins(newPlugins);
	}
	
	// Update reportPlugin
	if (model.getReporting() != null
		&& model.getReporting().getPlugins() != null) {
	    List<ReportPlugin> rplugins = model.getReporting().getPlugins();
	    List<ReportPlugin> newRPlugins = new ArrayList<ReportPlugin>();
	    for (ReportPlugin rp : rplugins) {
		if (rp.getGroupId().equals("org.universAAL.support")
			&& rp.getArtifactId().equals(
				"uaalDirectives-maven-plugin")) {
		    rp.setVersion(ask4NewVersion(rp.getGroupId(),
			    rp.getArtifactId(), rp.getVersion()));
		}
		newRPlugins.add(rp);
	    }
	    model.getReporting().setPlugins(newRPlugins);
	}
	
	// update samples ont.tutorial
	try {
	    if (model.getGroupId().equals("org.universAAL.ontology")
	    		&& model.getArtifactId().equals("ont.tutorial")){
	    	Parent p = model.getParent();
	    	p.setVersion(ask4NewVersion(p.getGroupId(),
	    			p.getArtifactId(), p.getVersion()));
	    	model.setParent(p);
	    }
	} catch (Exception e) {
	}
    }

    /**
     * Ask the user for the new version for the given artifact.
     * @param groupID
     * @param artifactID
     * @param currentVersion
     * @return
     */
    protected String ask4NewVersion(String groupID, String artifactID,
	    String currentVersion) {
	String newVersion;
	System.out
		.println("\nEnter new version for "
			+ groupID
			+ "/"
			+ artifactID
			+ "/"
			+ currentVersion
			+ " \n [enter version and press enter, press enter to keep current]");
	newVersion = System.console().readLine();
	if (newVersion == null || newVersion.isEmpty()) {
	    newVersion = currentVersion;
	}
	return newVersion;
    }

}
