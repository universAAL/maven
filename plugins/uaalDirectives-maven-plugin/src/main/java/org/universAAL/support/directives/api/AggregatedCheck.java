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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public abstract class AggregatedCheck implements APIFixableCheck {
	
	private List<APICheck> checks;
	
	public AggregatedCheck(){
		checks = getCheckList();
	}
	
	public abstract List<APICheck> getCheckList();

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenproject, Log log)
			throws MojoExecutionException, MojoFailureException {
		String fMessage = "";
		String eMessage ="";
		for (APICheck c : checks) {
			try {
				c.check(mavenproject, log);
			} catch (MojoFailureException fe) {
				fMessage += "\n" + fe.getMessage();
			} catch (MojoExecutionException ee) {
				eMessage += "\n" + ee.getMessage();
			}
		}
		if (!eMessage.isEmpty()){
			throw new MojoExecutionException(eMessage);
		}
		if (!fMessage.isEmpty()){
			throw new MojoFailureException(fMessage);
		}
		return fMessage.isEmpty() && eMessage.isEmpty();
	}

	/** {@inheritDoc} */
	public void fix(MavenProject mavenProject, Log log) 
			throws MojoExecutionException, MojoFailureException {
		String fMessage = "";
		String eMessage ="";
		for (APICheck c : checks) {
			if (c instanceof APIFixableCheck) {
				try {
					((APIFixableCheck)c).fix(mavenProject, log);
				}  catch (MojoFailureException fe) {
					fMessage += "\n" + fe.getMessage();
				} catch (MojoExecutionException ee) {
					eMessage += "\n" + ee.getMessage();
				}
			}
		}

		if (!eMessage.isEmpty()){
			throw new MojoExecutionException(eMessage);
		}
		if (!fMessage.isEmpty()){
			throw new MojoFailureException(fMessage);
		}
		
	}
	
}