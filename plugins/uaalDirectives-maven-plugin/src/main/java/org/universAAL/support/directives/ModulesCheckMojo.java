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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * @author amedrano
 * 
 * @aggregator
 * 
 * @goal modules-check
 * 
 * @phase process-sources
 */
public class ModulesCheckMojo extends AbstractMojo implements PomFixer{
	/**
	 * Message content when check fails
	 */
	private static final String MODULES_NOT_CONFIGURED_ROOT = System
			.getProperty("line.separator")
			+ "\n"
			+ "Modules List Directive Fail :\n"
			+ "It seems the POM does not list all the modules it should. "
			+ System.getProperty("line.separator") + "\n";

	/**
	 * @parameter expression="${failOnMissMatch}" default-value="false"
	 */
	private boolean failOnMissMatch;

	/**
	 * @parameter expression="${directive.fix}" default-value="false"
	 */
	private boolean fixVersions;

	/** @parameter default-value="${project}" */
	private org.apache.maven.project.MavenProject mavenProject;

   /**
    * The projects in the reactor.
    *
    * @parameter expression="${reactorProjects}"
    * @readonly
    */
   private List<MavenProject> reactorProjects;
   
   /**
    * List of Dependencies to be fixed
    */
   private ArrayList<String> toBeFixed = new ArrayList<String>();
	
	public void execute() throws MojoFailureException {
		if (!passCheck(mavenProject)) {
			String err = getErrorMessge();
			if (failOnMissMatch) {
				throw new MojoFailureException(err);
			} else {
				getLog().warn(err);
			}
			if (fixVersions) {
				getLog().info("Fixing Modules.");
				fix(mavenProject);
			}
		} else {
			getLog().info("Module list is Correct.");
		}
	}

	private String getErrorMessge() {
		if (DirectiveCheckMojo.isRootProject(mavenProject)) {
			String err = MODULES_NOT_CONFIGURED_ROOT;
			for (String mod : toBeFixed) {
				err += "\n" + mod 
						+ ", version should be listed as a module." ;
			}
			return err;
		}
		return "";
	}

	/**
	 * fix a Dependency management
	 * @param mavenProject2
	 */
	private void fix(MavenProject mavenProject2) {
		// TODO Auto-generated method stub
		try {
			new PomWriter(this, mavenProject2).fix();
		} catch (Exception e) {
			getLog().error("unable to Write POM.");
		}
	}

	/**
	 * check whether there are any versions defined or dependencyManagement points to correct versions
	 * @param mavenProject2
	 * @return
	 */
	private boolean passCheck(MavenProject mavenProject2) {
		if (mavenProject2.getPackaging().equals("pom")) {
			return passRootCheck(mavenProject2);
		}
		else {
			return true;
		}
	}

	private boolean passRootCheck(MavenProject mavenProject2) {
		List<String> listed = (List<String>) mavenProject2.getModules();
		
		//gather the existent modules
		File dir = mavenProject2.getBasedir().getParentFile();
		for (File f : dir.listFiles()) {
			String rel = "../" + f.getName();
			if (f.isDirectory()
					&& !listed.contains(rel)
					&& !rel.endsWith(mavenProject2.getBasedir().getName())) {
				toBeFixed.add(rel);
				getLog().debug("Found not listed module : " + rel);
			}
		}
		return toBeFixed.isEmpty();
	}
	
	public void fix(Model model) {
		for (String mod: toBeFixed) {
			model.addModule(mod);
		}
	}
}
