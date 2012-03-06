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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Tags the project in an appropiate tag URL, in concordance to T2.3 Directives.
 * @author amedrano
 * 
 * @goal tag
 * 
 * @requiresDirectInvocation 
 */
public class TagMojo extends AbstractMojo {

    /**
     * Message content when tag fails
     */
    private static final String NOT_TAGGED = System
	    .getProperty("line.separator")
	    + "\n"
	    + "Failed Trying to tag the project try again manually.\n"
	    + System.getProperty("line.separator") + "\n";


    /** @parameter default-value="${project}" */
    private MavenProject mavenProject;
    
    /** {@inheritDoc} */
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info(getTagURL());
	}
	
	/**
	 * parses the scm url to generate an appropiate tag URL, in concordance to T2.3 Directives
	 * @return
	 */
	public String getTagURL() {
		String scmUrl = mavenProject.getScm().getDeveloperConnection();
		scmUrl = scmUrl.replace("scm:", "");
		scmUrl = scmUrl.replace("svn:", "");
		String tagUrl = scmUrl.split("trunk")[0];
		tagUrl += "tags/";
		getLog().debug("tagUrl1 :" +tagUrl);
		if (DirectiveCheckMojo.isSnapshot(mavenProject)) {
			tagUrl += "SNAPSHOT/" + mavenProject.getArtifactId() + "-" + mavenProject.getVersion();
		}
		else {
			tagUrl += mavenProject.getVersion()+ "/" + mavenProject.getArtifactId();
		}
		getLog().debug(tagUrl);
		return tagUrl;		
	}
	
	public void performTag(String url, String tagUrl, String msg) {
		SVNClientManager cli = SVNClientManager.newInstance();
		try {
			SVNCopySource source = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, SVNURL.parseURIDecoded(url));
			cli.getCopyClient().doCopy(new SVNCopySource[]{ source},
					SVNURL.parseURIDecoded(tagUrl),
					false, true, true, msg, new SVNProperties());
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
