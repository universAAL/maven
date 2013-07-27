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
package org.universAAL.support.directives.procedures;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.universAAL.support.directives.api.APIProcedure;
import org.universAAL.support.directives.api.AbstractCheckMojo;

/**
 * Tags the project in an appropiate tag URL, in concordance to T2.3 Directives.
 * @author amedrano
 */
public class Tag implements APIProcedure{

    /**
     * Message content when tag fails
     */
    private static final String NOT_TAGGED = 
	    "Failed Trying to tag the project try again manually.\n";


    private boolean tagRemoteHead;
    
    
    public Tag(boolean tagRemoteHead) {
		super();
		this.tagRemoteHead = tagRemoteHead;
	}

	/** {@inheritDoc} */
	public void execute(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {
		String url = mavenProject.getScm().getDeveloperConnection();
		String tagUrl = getTagURL(mavenProject);
		log.info("Tagging: " + url + "  ->  " + tagUrl);
		if (tagRemoteHead) {
			if (!performTag(url, tagUrl, "Automatic tag of " 
					+ mavenProject.getArtifactId() + " version: " + mavenProject.getVersion())) {
				throw new MojoExecutionException(NOT_TAGGED);
			}			
		}
		else {
			if (!performWCTag(mavenProject.getBasedir(), tagUrl, "Automatic tag of " 
					+ mavenProject.getArtifactId() + " version: " + mavenProject.getVersion())) {
				throw new MojoExecutionException(NOT_TAGGED);
			}
		}
	}
	
	/**
	 * parses the scm url to generate an appropiate tag URL, in concordance to T2.3 Directives
	 * @return
	 */
	public static String getTagURL(MavenProject mavenProject) {
		String scmUrl = mavenProject.getScm().getDeveloperConnection();
		scmUrl = scmUrl.replace("scm:", "").replace("svn:", "");
		String tagUrl = scmUrl.split("trunk")[0];
		tagUrl += "tags/";
		if (AbstractCheckMojo.isSnapshot(mavenProject)) {
			tagUrl += "SNAPSHOT/" + mavenProject.getArtifactId() + "-" + mavenProject.getVersion();
		}
		else {
			tagUrl += mavenProject.getVersion() + scmUrl.split("trunk")[1];
		}
		return tagUrl;		
	}
	
	public boolean performTag(String url, String tagUrl, String msg) {
		try {
			SVNCopySource source = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, SVNURL.parseURIDecoded(url));
			return doTag(source, tagUrl, msg);
		} catch (SVNException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private static boolean doTag(SVNCopySource source, String tagUrl, String msg) throws SVNException {
		SVNClientManager cli = SVNClientManager.newInstance();
		cli.getCopyClient().doCopy(new SVNCopySource[]{ source},
				SVNURL.parseURIDecoded(tagUrl),
				false, true, true, msg, null);
		return true;
	}
	
	public boolean performWCTag(File wd, String tagUrl, String msg) {
		try {
			SVNCopySource source = new SVNCopySource(SVNRevision.WORKING, SVNRevision.WORKING, wd);			
			return doTag(source, tagUrl, msg);
		} catch (SVNException e) {
			e.printStackTrace();
			return false;
		}
	}
}
