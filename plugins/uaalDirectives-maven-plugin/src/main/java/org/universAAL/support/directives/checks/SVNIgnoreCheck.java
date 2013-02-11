/*******************************************************************************
 * Copyright 2011 Universidad Polit�cnica de Madrid
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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.universAAL.support.directives.api.APIFixableCheck;

/**
 * @author amedrano
 */
public class SVNIgnoreCheck implements APIFixableCheck {

	private String NO_IGNORES = System.getProperty("line.separator")
			+ "\n"
			+ "SVN ignore Directive Fail :\n"
			+ "It seems the current Directory includes files that should be ingored.\n "
			+ "Remember to ignore these files with your svn client before you commit:\n";

	/**
	 * @parameter alias="ignores"
	 */
	private String[] ignores;
	
	private static String[] DEFAULT_IGNORES = {".project", ".settings", "target", ".classpath"};

	private SVNWCClient wcCli;

	private String prop;

	private Log log;


	public SVNIgnoreCheck() {
		if (ignores == null
				|| ignores.length <= 0) {
			ignores = DEFAULT_IGNORES;
		}
		for (int i = 0; i < ignores.length; i++) {
			NO_IGNORES += ignores[i] + "\n";
		}
		NO_IGNORES += System.getProperty("line.separator");
	}

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {
		this.log = log;
		SVNClientManager cli = SVNClientManager.newInstance();
		log.debug(
				"checking svn ignore Rules in: "
						+ mavenProject.getBasedir().getPath());
		try {
			wcCli = cli.getWCClient();
			SVNPropertyData pd = wcCli.doGetProperty(mavenProject.getBasedir(),
					SVNProperty.IGNORE, SVNRevision.WORKING,
					SVNRevision.WORKING);
			boolean changed = false;
			prop = pd.getValue().getString();
			log.debug("Ignore Property contains: " + prop);// .split("\n")[0]
			for (int i = 0; i < ignores.length; i++) {
				if (!prop.contains(ignores[i]) && exists(mavenProject, ignores[i])) {
					prop += ignores[i] + "\n";
					changed = true;
				}
			}
			if (changed) {
				throw new MojoFailureException(NO_IGNORES);
			}
		} catch (SVNException e) {
			e.printStackTrace();
			log.warn("SVN Error.");
			log.warn("directory seems not to be a local SVN working copy.");
			return false;
		}
		return true;
	}

	private boolean exists(MavenProject mavenProject, String string) {
		String[] files = mavenProject.getBasedir().list();
		int i = 0;
		log.debug("Matching: " + string);
		while (i < files.length && !files[i].endsWith(string.replace("*", ""))) {
			log.debug("No Match: " + files[i] + " with " + string);
			i++;
		}
		if (i < files.length) {
			return files[i].endsWith(string.replace("*", ""));
		} 
		else {
			return false;
		}
	}

	/** {@inheritDoc} */
	public void fix(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {
		SVNPropertyValue propValue = SVNPropertyValue.create(SVNProperty.IGNORE, prop.getBytes(Charset.defaultCharset()));
		Collection<String> cl = new ArrayList<String>();
		cl.add("added ignore list");
		try {
			wcCli.doSetProperty(mavenProject.getBasedir(), SVNProperty.IGNORE, propValue, false, SVNDepth.IMMEDIATES, null, cl );
		} catch (SVNException e) {
			throw new MojoExecutionException("error setting SVN properties.", e);
		}
		log.info("Fixing");

	}

}
