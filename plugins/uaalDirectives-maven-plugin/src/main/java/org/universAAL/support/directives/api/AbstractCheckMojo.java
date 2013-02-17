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
package org.universAAL.support.directives.api;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Abstract Mojo that performs a {@link APICheck}
 * @author amedrano
 *
 */
public abstract class AbstractCheckMojo extends AbstractMojo {
	
	/**
	 * 
	 */
	private static final String CHECK_FAILED = "Check Failed.";

	/**
	 * @parameter expression="${failOnMissMatch}" default-value="false"
	 */
	private boolean failOnMissMatch;

	/** @parameter default-value="${project}" */
	private org.apache.maven.project.MavenProject mavenProject;

	protected boolean fail;

	protected APICheck check;
	
	/** {@inheritDoc} */
	public void execute() throws MojoExecutionException, MojoFailureException {
		check = getCheck();
		fail = false;
		AbstractMojoExecutionException failedE = null;
		
		try {
			if (!check.check(mavenProject, getLog())) {
				fail = true;
			}
		} catch (AbstractMojoExecutionException e) {
			fail = true;
			failedE= e;
		}
		
		if (fail && failOnMissMatch) {
			if (failedE == null) {
				throw new MojoFailureException(CHECK_FAILED);
			} else if (failedE instanceof MojoExecutionException) {
				throw (MojoExecutionException) failedE;
			} else if (failedE instanceof MojoFailureException) {
				throw (MojoFailureException) failedE;
			}
		} else if (fail) {
			if (failedE == null) {
				getLog().warn(CHECK_FAILED);
			} else {
				getLog().warn(failedE.getMessage() + ":\n\t" + failedE.getLongMessage());
			}
		}

	}
	
	public abstract APICheck getCheck();
	
	/**
	 * Check whether the project is at snapshot version
	 * @param mp The {@link MavenProject} descriptor
	 * @return True if the version contains SNAPSHOT
	 */
	static public boolean isSnapshot(MavenProject mp) {
		return mp.getVersion().contains("SNAPSHOT");
	}

}
