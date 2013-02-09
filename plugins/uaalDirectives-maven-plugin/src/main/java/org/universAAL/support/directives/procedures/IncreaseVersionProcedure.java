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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APIProcedure;

/**
 * @author amedrano
 *
 */
public class IncreaseVersionProcedure implements APIProcedure {

	/** {@inheritDoc} */
	public void execute(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {
		new ChangeVersionProcedure(nextDevelopmentVersion(mavenProject.getVersion()))
		.execute(mavenProject, log);

	}
	
	public static String nextDevelopmentVersion(String version) {
		String[] numbers =  version.replace("-SNAPSHOT", "").split("\\.");
		String newVersion = numbers[0] + '.' + numbers[1] + '.' 
				+ Integer.toString((Integer.parseInt(numbers[2]) + 1))
				+ "-SNAPSHOT";
		return newVersion;
	}

}
