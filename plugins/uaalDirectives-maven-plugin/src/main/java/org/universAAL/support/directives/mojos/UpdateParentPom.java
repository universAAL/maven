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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.api.APIProcedure;
import org.universAAL.support.directives.api.AbstractProcedureMojo;
import org.universAAL.support.directives.api.AggregatedCheck;
import org.universAAL.support.directives.checks.DependencyManagementCheckFix;
import org.universAAL.support.directives.checks.ModulesCheckFix;


/**
 * @author amedrano
 * @goal update-root-children
 */
public class UpdateParentPom extends AbstractProcedureMojo {

    /**
     * The projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List<MavenProject> reactorProjects;
	
	@Override
	public APIProcedure getProcedure() {
		return new UpdateProcedure();
	}

	public class UpdateProcedure extends AggregatedCheck implements APIProcedure {

		public void execute(MavenProject mavenProject, Log log)
				throws MojoExecutionException, MojoFailureException {
			try {
				this.check(mavenProject, log);
			} catch (Exception e) {
			}
			this.fix(mavenProject, log);			
		}

		@Override
		public List<APICheck> getCheckList() {
			List<APICheck> l = new ArrayList<APICheck>();
			l.add(new ModulesCheckFix());
			l.add(new DependencyManagementCheckFix(reactorProjects));
			return null;
		}
		
	}

}
