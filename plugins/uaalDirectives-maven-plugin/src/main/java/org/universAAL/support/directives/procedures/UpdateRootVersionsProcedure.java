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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APIProcedure;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * @author amedrano
 *
 */
public class UpdateRootVersionsProcedure implements APIProcedure, PomFixer {

    private static final String UAAL_GID = "org.universAAL";

    private static final String UAAL_AID = "uAAL.pom";
	
	/**
	 * the version to change uAAL.pom and root imports to.
	 */
	private String newVersion;
	
	public UpdateRootVersionsProcedure(String newVersion) {
		super();
		this.newVersion = newVersion;
	}

	/** {@inheritDoc} */
	public void execute(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {

		try {
			log.info("Changing uAAL version and root imports to: " + newVersion);
			new PomWriter(this, mavenProject).fix();
		} catch (Exception e) {
			throw new MojoExecutionException("unable to fix Version", e);
		}

	}

	public void fix(Model model) {
		if (model.getParent().getArtifactId().equals(UAAL_AID)
				&& model.getParent().getGroupId().equals(UAAL_GID)){
			model.getParent().setVersion(newVersion);
		}
		if (model.getPackaging().equals("pom")){
			List<Dependency> deps = model.getDependencyManagement().getDependencies();
			List<Dependency> nld = new ArrayList<Dependency>();
			for (Dependency d : deps) {
				if (d.getScope() != null
						&& d.getScope().equals("import")
						&& d.getGroupId().startsWith(UAAL_GID)){
					d.setVersion(newVersion);
				}
				nld.add(d);			
			}
			model.getDependencyManagement().setDependencies(nld);
		}
	}

}
