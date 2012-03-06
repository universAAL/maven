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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @author amedrano
 * 
 * @goal check
 * 
 * @execute goal="svn-check"
 * @execute goal="svnIgnore-check"
 */
public class DirectiveCheckMojo extends AbstractMojo {

	private static final Object UAAL_SUPER_POM_AID = "uAAL.pom";
	private static final Object UAAL_SUPER_POM_GID = "org.universAAL";

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Validated to all applicable directives...");
	}

	
	/**
	 * Check whether the {@link MavenProject} is one of the root universAAL root projects.
	 * @param mp
	 * 		The {@link MavenProject} descriptor
	 * @return
	 * 		True if it's direct child of uaal.pom
	 */
	static public boolean isRootProject(MavenProject mp) {
		return mp.getParent().getArtifactId().equals(UAAL_SUPER_POM_AID)
				&& mp.getParent().getGroupId().equals(UAAL_SUPER_POM_GID);
	}
	
	/**
	 * Check whether the project is at snapshot version
	 * @param mp The {@link MavenProject} descriptor
	 * @return True if the version contains SNAPSHOT
	 */
	static public boolean isSnapshot(MavenProject mp) {
		return mp.getVersion().contains("SNAPSHOT");
	}
}
