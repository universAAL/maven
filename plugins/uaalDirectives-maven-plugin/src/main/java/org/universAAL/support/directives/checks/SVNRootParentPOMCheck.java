/*******************************************************************************
 * Copyright 2011 Universidad Politécnica de Madrid
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
package org.universAAL.support.directives.checks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.universAAL.support.directives.api.APIFixableCheck;

/**
 * @author amedrano
 */
public class SVNRootParentPOMCheck implements APIFixableCheck {

    /**
     * Variable to check
     */
    private static final String PROP = "gforge.project.name";
    
    /**
     * Message content when check fails
     */
    private static final String VARIABLE_NOT_CONFIGURED = System
	    .getProperty("line.separator")
	    + "\n"
	    + "Parent Property Conformance Directive Fail :\n"
	    + "It seems the POM does not contain a <" + PROP + "> property, "
	    + "add such property, which must be equal to gforge's short project name."
	    + System.getProperty("line.separator") + "\n";
    
    private static final Object UAAL_GID = "org.universAAL";

    private static final String UAAL_AID = "uAAL.pom";
    
    /** {@inheritDoc} */
    public boolean check(MavenProject mavenProject, Log log)
    		throws MojoExecutionException, MojoFailureException {
    	if (isParentRootPOM(mavenProject)
    			&& !hasProperty(mavenProject,PROP)){
    		throw new MojoFailureException(VARIABLE_NOT_CONFIGURED);
    	}
    	return true;
    }

    /** {@inheritDoc} */
	public void fix(MavenProject mavenProject, Log log)
			throws MojoExecutionException {
		String correctValue;
			try {
			    correctValue = getSVNURL(mavenProject.getBasedir());
			    URL u = new URL(correctValue);
			    correctValue = u.getPath().split("/")[1];
			    log.debug("Determined Correct Value for " + PROP + ": " + correctValue);
			    // Reading
			    MavenXpp3Reader reader = new MavenXpp3Reader();
			    Model model = reader.read(new FileInputStream(new File(mavenProject
				    .getFile().getAbsolutePath())));

			    // Editing
			    Properties p = model.getProperties();
			    
			    p.setProperty(PROP, correctValue);
			    model.setProperties(p);

			    // Writing
			    MavenXpp3Writer writer = new MavenXpp3Writer();

			    writer.write(new OutputStreamWriter(new FileOutputStream(new File(
				    mavenProject.getFile().getAbsolutePath()))), model);
			} catch (Exception e) {
			    log.error("Unable to Correct POM",e);
			}
		
	}

	public static boolean hasProperty(MavenProject mp, String prop) {
	return mp.getProperties().containsKey(prop);
    }

    public static boolean isParentRootPOM(MavenProject mavenProject2) {
	MavenProject parent = mavenProject2.getParent();
	return (parent.getGroupId().equals(UAAL_GID)
		&& parent.getArtifactId().endsWith(UAAL_AID));
    }
    
	public static String getSVNURL(File dir) throws Exception, SVNException {
		SVNClientManager cli = SVNClientManager.newInstance();
		SVNStatus status;
		status = cli.getStatusClient().doStatus(dir, false);
		if (status != null) {
			SVNURL url = status.getURL();
			return url.toDecodedString();
		}
		throw new Exception("unable to find URL from svn info.");
	}
}
