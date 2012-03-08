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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * @author amedrano
 * 
 * @goal dependency-check
 * 
 * @phase process-sources
 */
public class DepManagementCheckMojo extends AbstractMojo implements PomFixer{
	/**
	 * Message content when check fails
	 */
	private static final String VERSIONS_NOT_CONFIGURED_ROOT = System
			.getProperty("line.separator")
			+ "\n"
			+ "dependencyManagement Conformance Directive Fail :\n"
			+ "It seems the POM has a dependencyManagement malformed section. "
			+ System.getProperty("line.separator") + "\n";
	/**
	 * Message content when check fails
	 */
	private static final String VERSIONS_NOT_CONFIGURED = System
			.getProperty("line.separator")
			+ "\n"
			+ "dependencyManagement Conformance Directive Fail :\n"
			+ "It seems the POM has versions it shouldn't. "
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
   private HashMap<String, String> toBeFixed = new HashMap<String, String>();
	
	public void execute() throws MojoFailureException {
		if (!passCheck(mavenProject)) {
			String err = getErrorMessge();
			if (failOnMissMatch) {
				throw new MojoFailureException(err);
			} else {
				getLog().warn(err);
			}
			if (fixVersions) {
				getLog().info("Fixing Versions.");
				fix(mavenProject);
			}
		} else {
			getLog().info("Versions are Correct.");
		}
	}

	private String getErrorMessge() {
		String err = DirectiveCheckMojo.isRootProject(mavenProject)? 
				VERSIONS_NOT_CONFIGURED_ROOT 
				: VERSIONS_NOT_CONFIGURED;
		if (!toBeFixed.isEmpty()) {
			for (Iterator<String> iterator = toBeFixed.keySet().iterator(); iterator.hasNext();) {
				String dep =  (String) iterator.next();
				err += "\n" + dep 
						+ ", version should be : " + toBeFixed.get(dep) ;
			}
		}
		return err;
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
			return passNoRootCheck(mavenProject2);
		}
	}

	private boolean passNoRootCheck(MavenProject mavenProject2) {
		// TODO check that the pom (not the model) hasn't any versions in it.
		return true;
	}

	private boolean passRootCheck(MavenProject mavenProject2) {
		HashMap<String,String> versionMap = getActualVersions(mavenProject2);
		List<Dependency> lod = mavenProject.getDependencyManagement().getDependencies();
		for (Iterator<Dependency> iterator = lod.iterator(); iterator.hasNext();) {
			Dependency dependency = (Dependency) iterator.next();
			String realVersion = versionMap.get(dependency.getGroupId() + ":" +  dependency.getArtifactId());
			if ( dependency != null &&
					! dependency.getVersion()
					.equals(realVersion)
					&& realVersion != null) {
				toBeFixed.put(dependency.getGroupId() + ":" + dependency.getArtifactId(), realVersion);
			}
		}
		for (Iterator<?> iterator = versionMap.entrySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			if (!lod.contains(key)) {
				toBeFixed.put(key, versionMap.get(key));
			}
		}
		return toBeFixed.isEmpty();
	}

	private HashMap<String, String> getActualVersions(MavenProject mavenProject2) {
		HashMap<String,String> versionMap = new HashMap<String, String>();
		for (Iterator<MavenProject> iterator = reactorProjects.iterator(); iterator.hasNext();) {
			MavenProject mavenProject = (MavenProject) iterator.next();
			if (mavenProject.getVersion() != null) {
				versionMap.put(mavenProject.getGroupId()+ ":" + mavenProject.getArtifactId()
						,mavenProject.getVersion());
				getLog().debug("added to ActualVersions: " + mavenProject.getGroupId() + ":" + mavenProject.getArtifactId()
						+ mavenProject.getVersion());
			}
		}
		return versionMap;
	}

	public void fix(Model model) {
		List<Dependency> dep = model.getDependencyManagement().getDependencies();
		List<Dependency> newDep = new ArrayList<Dependency>();
		getLog().debug(Integer.toString(dep.size())+"\n");
		for (Iterator<Dependency> iterator = dep.iterator(); iterator.hasNext();) {
			Dependency dependency = (Dependency) iterator.next();
			String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
			if (toBeFixed.containsKey(key)) {
				dependency.setVersion(toBeFixed.get(key));
				getLog().info("Fixing: " + dependency.getGroupId() + ":" + dependency.getArtifactId()
						+ " to: " + toBeFixed.get(key));
				toBeFixed.remove(key);
			}
			newDep.add(dependency);
		}
		for (Iterator<String> iterator = toBeFixed.keySet().iterator(); iterator.hasNext();) {
			String depIDs = (String) iterator.next();
			Dependency d = new Dependency();
			String[] ids = depIDs.split("\\:");
			d.setArtifactId(ids[0]);
			d.setGroupId(ids[1]);
			d.setVersion(toBeFixed.get(depIDs));
			newDep.add(d);
		}
		model.getDependencyManagement().setDependencies(newDep);		
	}
}
