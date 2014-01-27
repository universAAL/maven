/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 * Copyright 2013 Fraunhofer-Gesellschaft - Institute for Computer Graphics Research
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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * @author amedrano
 *
 */
public class FelixDepCheckFix implements APIFixableCheck,PomFixer{

    /** {@ inheritDoc}	 */
    public boolean check(MavenProject mavenproject, Log log)
	    throws MojoExecutionException, MojoFailureException {
	
	List<Dependency> deps = mavenproject.getDependencies();
	for (Dependency d : deps) {
	    if (d.getGroupId().contains("felix")){
		log.info("felix dependency found");
		return false;
	    }
	}
	return true;
    }

    /** {@ inheritDoc}	 */
    public void fix(MavenProject mavenProject, Log log)
	    throws MojoExecutionException, MojoFailureException {

	try {
		new PomWriter(this, mavenProject).fix();
	} catch (Exception e) {
		log.error("unable to Write POM.");
		log.error(e);
	}
	
	
    }

    /** {@ inheritDoc}	 */
    public void fix(Model model) {
	List<Dependency> deps = model.getDependencies();
	List<Dependency> newDeps = new ArrayList<Dependency>();
	for (Dependency d : deps) {
	    if (d.getGroupId().equals("org.apache.felix")
		    && (d.getArtifactId().equals("org.osgi.core") 
			    || d.getArtifactId().equals("org.osgi.compendium"))){
		d.setGroupId("org.osgi");
	    }
	    newDeps.add(d);
	}
	model.setDependencies(newDeps);
    }

}
