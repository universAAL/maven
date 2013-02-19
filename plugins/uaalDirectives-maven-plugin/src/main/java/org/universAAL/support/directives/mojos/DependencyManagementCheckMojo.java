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
package org.universAAL.support.directives.mojos;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProjectBuilder;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.api.AbstractFixableCheckMojo;
import org.universAAL.support.directives.checks.DependencyManagementCheckFix;

/**
 * This Mojo checks (and fixes, if configured to do so) Dependency and DependencyManagement sections.
 * <p>
 * For parent POMs dependencyManagement section should contain all of it's modules as dependencies, 
 * and the versions of these dependencies should match the actual version in the module's POM.
 * <p>
 * For project POMs there should not be any dependency with a version already managed by the inherited (or not) 
 * dependencyManagement section.
 * @author amedrano
 * 
 * @goal dependency-check
 * 
 * @phase process-sources
 */
public class DependencyManagementCheckMojo extends AbstractFixableCheckMojo {
	
    /** 
     * The projectBuilder to build children modules.
     * @component 
     */
	private MavenProjectBuilder mavenProjectBuilder;
	
	/**
	 * The localRepository reference, necessary to build projects.
	 * @parameter default-value="${localRepository}" 
	 */
	private ArtifactRepository localRepository;
	
	/** {@inheritDoc} */
	@Override
	public APIFixableCheck getFix() {
		return new DependencyManagementCheckFix(mavenProjectBuilder,
				localRepository);
	}

}
