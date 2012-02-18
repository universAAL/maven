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
package org.universAAL.support.directives;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNStatus;

/**
 * @author amedrano
 * 
 * @goal svn-check
 * 
 * @phase process-sources
 */
public class SVNCheckMojo extends AbstractMojo {

    private static final String NEW_URL = "forge.universAAL.org";

    private static final String OLD_URL = "a1gforge.igd.fraunhofer.de";

    /**
     * Message content when check fails
     */
    private static final String SCM_NOT_CONFIGURED = System
	    .getProperty("line.separator")
	    + "\n"
	    + "SCM Conformance Directive Fail :\n"
	    + "It seems the POM does not contain a SCM tag, "
	    + "or the SCM does not match the actual SVN repository URL."
	    + System.getProperty("line.separator") + "\n";

    /**
     * @parameter expression="${failOnMissMatch}" default-value="false"
     */
    private boolean failOnMissMatch;

    /**
     * @parameter expression="${directive.fix}" default-value="false"
     */
    private boolean fixSCM;

    /** @parameter default-value="${project}" */
    private org.apache.maven.project.MavenProject mavenProject;

    public void execute() throws MojoFailureException {
	this.getLog().debug(
		"checking svn for " + mavenProject.getBasedir().getPath());
	try {
	    String surl = getSVNURL(mavenProject.getBasedir());

	    getLog().debug("found URL	: " + surl);
	    getLog().debug(
		    "comparing with	: " + mavenProject.getScm().getConnection());
	    getLog().debug(
		    "comparing with	: "
			    + mavenProject.getScm().getDeveloperConnection());
	    if (missMatchURLs(surl)
		    || missMatchURLs(surl.replace(OLD_URL, NEW_URL))) {
		if (failOnMissMatch) {
		    throw new MojoFailureException(SCM_NOT_CONFIGURED);
		} else {
		    getLog().warn(SCM_NOT_CONFIGURED);
		}
		if (fixSCM) {
		    getLog().debug("Fixing SCM with URL: " + surl);
		    fixSCMWith(surl);
		}
	    } else {
		getLog().info("SCM and SVN info are in sync.");
	    }
	} catch (SVNException e) {
	    getLog().warn("SVN Error.", e);
	    getLog().warn("directory seems not to be a local SVN working copy.");
	}
	catch (Exception e1){
	    getLog().error(e1);
	}

    }

    private void fixSCMWith(String surl) {
	mavenProject.getScm().setConnection(surl);
	mavenProject.getScm().setDeveloperConnection(surl);
	// rewrite POM with correct SCM.
	try {
	    // Reading
	    MavenXpp3Reader reader = new MavenXpp3Reader();
	    Model model = reader.read(new FileInputStream(new File(mavenProject
		    .getFile().getAbsolutePath())));

	    // Editing
	    model.setScm(mavenProject.getScm());

	    // Writing
	    MavenXpp3Writer writer = new MavenXpp3Writer();

	    writer.write(new OutputStreamWriter(new FileOutputStream(new File(
		    mavenProject.getFile().getAbsolutePath()))), model);
	} catch (Exception e) {
	    getLog().error("Unable to write POM");
	}
    }

    private boolean missMatchURLs(String url) {
	return !mavenProject.getScm().getConnection().endsWith(url)
		&& !mavenProject.getScm().getDeveloperConnection()
			.endsWith(url);
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
