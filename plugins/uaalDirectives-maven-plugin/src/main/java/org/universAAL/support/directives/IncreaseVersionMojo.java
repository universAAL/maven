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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * Tags the project in an appropiate tag URL, in concordance to T2.3 Directives.
 * @author amedrano
 * 
 * @goal increase-version
 * 
 * @requiresDirectInvocation 
 */
public class IncreaseVersionMojo extends AbstractMojo implements PomFixer{

    /** @parameter default-value="${project}" */
    private MavenProject mavenProject;
    
    /** {@inheritDoc} */
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			
			new PomWriter(this, mavenProject).fix();
		} catch (Exception e) {
			throw new MojoExecutionException("unable to fix Version");
		}
	}

	public void fix(Model model) {
		String newVersion = nextDevelopmentVersion(mavenProject.getVersion());
		getLog().info("Changing version from: " + mavenProject.getVersion() + " to: " + newVersion);
		model.setVersion(newVersion);		
	}

	public static String nextDevelopmentVersion(String version) {
		String[] numbers =  version.replace("-SNAPSHOT", "").split("\\.");
		String newVersion = numbers[0] + '.' + numbers[1] + '.' 
				+ Integer.toString((Integer.parseInt(numbers[2]) + 1))
				+ "-SNAPSHOT";
		return newVersion;
	}
	
}
