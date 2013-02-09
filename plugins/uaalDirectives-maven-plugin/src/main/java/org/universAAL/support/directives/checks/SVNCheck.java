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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * @author amedrano
 */
public class SVNCheck implements APIFixableCheck {

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
	 * The SVN url.
	 */
	private String surl;

	/**
	 * the instance of {@link MavenProject}
	 */
	private MavenProject mavenProject;


	public boolean check(MavenProject mavenProject, Log log) {
		this.mavenProject = mavenProject;
		log.debug(
				"checking svn for " + mavenProject.getBasedir().getPath());
		try {
			surl = getSVNURL(mavenProject.getBasedir());

			log.debug("found URL	: " + surl);
			log.debug(
					"comparing with	: " + mavenProject.getScm().getConnection());
			log.debug(
					"comparing with	: "
							+ mavenProject.getScm().getDeveloperConnection());
			if (missMatchURLs(surl)
					|| missMatchURLs(surl.replace(OLD_URL, NEW_URL))) {
				throw new MojoFailureException(SCM_NOT_CONFIGURED);
				
			} else {
				log.info("SCM and SVN info are in sync.");
				return true;
			}
		} catch (SVNException e) {
			log.warn("SVN Error.", e);
			log.warn("directory seems not to be a local SVN working copy.");
		} catch (Exception e1) {
			log.error(e1);
		}
		return false;
	}
	
	public void fix(MavenProject mavenProject, Log log) {
		log.debug("Fixing SCM with URL: " + surl);
		fixSCMWith(surl, log);		
	}

	private void fixSCMWith(String surl, Log log) {
		try {
			new PomWriter(new SCMFixer(surl), mavenProject).fix();
		} catch (Exception e) {
			log.error("unable to write POM");
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
	
	private class SCMFixer implements PomFixer {
		private String surl;
		
		public SCMFixer(String urlFix) {
			surl = urlFix;
		}

		public void fix(Model model) {
			mavenProject.getScm().setConnection(surl);
			mavenProject.getScm().setDeveloperConnection(surl);
			model.setScm(mavenProject.getScm());
		}
	}
}
