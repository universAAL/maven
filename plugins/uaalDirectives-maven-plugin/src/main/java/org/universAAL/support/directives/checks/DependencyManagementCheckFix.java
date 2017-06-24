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
package org.universAAL.support.directives.checks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;

/**
 * @author amedrano
 *
 */
public class DependencyManagementCheckFix implements APIFixableCheck, PomFixer {

	/**
	 * Message content when check fails
	 */
	private static final String VERSIONS_NOT_CONFIGURED_ROOT = "dependencyManagement Conformance Directive Fail :\n"
			+ "It seems the POM has a dependencyManagement malformed section.";
	/**
	 * Message content when check fails
	 */
	private static final String VERSIONS_NOT_CONFIGURED = "dependencyManagement Conformance Directive Fail :\n"
			+ "It seems the POM has versions it shouldn't.";

	/**
	 * List of Dependencies to be fixed
	 */
	private Map<DependencyID, String> toBeFixed;

	/**
	 * list of children projects.
	 */
	private List<MavenProject> reactorProjects;

	private MavenProjectBuilder mavenProjectBuilder;
	private ArtifactRepository localRepository;

	public DependencyManagementCheckFix(MavenProjectBuilder mavenProjectBuilder, ArtifactRepository localRepository) {
		super();
		this.mavenProjectBuilder = mavenProjectBuilder;
		this.localRepository = localRepository;
	}

	/**
	 * Log instance.
	 */
	private Log log;

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenProject, Log log) throws MojoExecutionException, MojoFailureException {

		this.log = log;
		if (!passCheck(mavenProject)) {
			String err = getErrorMessge(mavenProject);
			throw new MojoFailureException(err);
		}
		return true;
	}

	/** {@inheritDoc} */
	public void fix(MavenProject mavenProject, Log log) throws MojoExecutionException, MojoFailureException {
		try {
			new PomWriter(this, mavenProject).fix();
		} catch (Exception e) {
			log.error("unable to Write POM.");
			log.error(e);
		}
	}

	private String getErrorMessge(MavenProject mavenProject) {
		String err;
		if (mavenProject.getPackaging().equals("pom")) {
			err = VERSIONS_NOT_CONFIGURED_ROOT;
			for (DependencyID dep : toBeFixed.keySet()) {
				err += "\n\t" + dep.getGID() + ":" + dep.getAID() + ", version should be : " + toBeFixed.get(dep);
			}
		} else {
			err = VERSIONS_NOT_CONFIGURED;
			for (DependencyID dep : toBeFixed.keySet()) {
				err += "\n\t" + dep.getGID() + ":" + dep.getAID() + ", version shouldn't be declared.";
			}
		}
		return err;
	}

	/**
	 * @return
	 */
	private Log getLog() {
		return this.log;
	}

	/**
	 * check whether there are any versions defined or dependencyManagement
	 * points to correct versions
	 *
	 * @param mavenProject2
	 * @return
	 * @throws Exception
	 *             when any of the children pom files can not be located.
	 */
	private boolean passCheck(MavenProject mavenProject2) {
		toBeFixed = new TreeMap<DependencyID, String>();
		reactorProjects = getChildrenModules(mavenProject2, mavenProjectBuilder, localRepository, null);
		if (mavenProject2.getPackaging().equals("pom")) {
			return passRootCheck(mavenProject2);
		} else {
			return passNoRootCheck(mavenProject2);
		}
	}

	private boolean passNoRootCheck(MavenProject mavenProject2) {
		// check that the pom (not the model) hasn't any versions in it.
		DependencyManagement dm = mavenProject2.getParent().getDependencyManagement();
		if (dm == null) // no dependency management -> no fixing
			return true;
		List<Dependency> depMan = dm.getDependencies();
		Map<DependencyID, String> depIDMan = new TreeMap<DependencyID, String>();
		// grather DependencyIDs form parent
		for (Dependency dep : depMan) {
			depIDMan.put(new DependencyID(dep), dep.getVersion());
		}
		try {
			for (Object o : PomWriter.readPOMFile(mavenProject2).getDependencies()) {
				Dependency dep = (Dependency) o;
				DependencyID depID = new DependencyID(dep);
				getLog().debug("***.1 " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion());
				getLog().debug("***.1 " + depID.getGID() + ":" + depID.getAID());
				if (depIDMan.containsKey(depID) && dep.getVersion() != null) {
					getLog().debug("in DepManagement. Declared Version: " + dep.getVersion() + "Managed Version: "
							+ depIDMan.get(depID));
					toBeFixed.put(depID, null);
				}
			}
		} catch (Exception e) {
		}
		return toBeFixed.isEmpty();
	}

	private boolean passRootCheck(MavenProject mavenProject2) {
		Map<DependencyID, String> versionMap = getActualVersions(mavenProject2);
		DependencyManagement dm = mavenProject2.getDependencyManagement();
		if (dm == null) // no DependencyManagement -> no fixing
			return true;
		List<Dependency> lod = dm.getDependencies();
		Map<DependencyID, String> lodVersionMap = new TreeMap<DependencyID, String>();

		// test if the version in DependencyManagement corresponds to the
		// version of the actual artefact
		for (Dependency dependency : lod) {
			DependencyID depId = new DependencyID(dependency);
			String realVersion = versionMap.get(depId);
			lodVersionMap.put(depId, dependency.getVersion());
			getLog().debug("***1 ." + dependency.getGroupId() + ":" + dependency.getArtifactId() + " Real:\""
					+ realVersion + "\" - Declared: \"" + dependency.getVersion() + "\"");
			if (dependency != null && !dependency.getVersion().equals(realVersion) && realVersion != null
					&& !realVersion.isEmpty()) {
				getLog().debug("Marked as wrong.");
				toBeFixed.put(new DependencyID(dependency), realVersion);
			}
		}

		// test that every real artefact has an entry in the
		// DependencyManagement
		for (DependencyID key : versionMap.keySet()) {
			if (!lodVersionMap.containsKey(key)) {
				toBeFixed.put(key, versionMap.get(key));
				getLog().debug("***2 ." + key.getGID() + ":" + key.getAID() + " Not declared.");
				// System.out.println("***2 ." + key + ". - ." +
				// versionMap.get(key) + ".");
			}
		}
		return toBeFixed.isEmpty();
	}

	private Map<DependencyID, String> getActualVersions(MavenProject mavenProject2) {
		TreeMap<DependencyID, String> versionMap = new TreeMap<DependencyID, String>();
		boolean containsSubPOMProjects = includesPOMSubProjects(mavenProject2);
		for (MavenProject mavenProject : reactorProjects) {
			if (mavenProject.getVersion() != null
					&& (!mavenProject.getPackaging().equals("pom") || containsSubPOMProjects)) {
				// Check if its a pom, add it if not!
				versionMap.put(new DependencyID(mavenProject.getGroupId(), mavenProject.getArtifactId()),
						mavenProject.getVersion());
				getLog().debug("added to ActualVersions: " + mavenProject.getGroupId() + ":"
						+ mavenProject.getArtifactId() + ":" + mavenProject.getVersion());
			}
		}
		return versionMap;
	}

	/**
	 * @param mavenProject2
	 * @return
	 */
	private boolean includesPOMSubProjects(MavenProject mavenProject2) {
		DependencyManagement dm = mavenProject2.getDependencyManagement();
		if (dm != null) {
			for (Dependency d : dm.getDependencies()) {
				if (d.getType().equals("pom") || d.getType().equals("cfg")) {
					return true;
				}
			}
		}
		return false;
	}

	public void fix(Model model) {
		if (model.getPackaging().equals("pom")) {
			fixPOM(model);
		} else {
			fixNonPOM(model);
		}
	}

	public void fixNonPOM(Model model) {
		List<Dependency> ld = model.getDependencies();
		List<Dependency> nld = new ArrayList<Dependency>();
		for (Dependency dep : ld) {
			boolean found = false;
			for (DependencyID depID : toBeFixed.keySet()) {
				if (depID.compareTo(new DependencyID(dep)) == 0) {
					nld.add(depID.toDependency());
					found = true;
				}
			}
			if (!found)
				nld.add(dep);
		}
		model.setDependencies(nld);
	}

	public void fixPOM(Model model) {
		List<Dependency> modelDependencyManagement = model.getDependencyManagement().getDependencies();
		List<Dependency> newDep = new ArrayList<Dependency>();
		getLog().debug(Integer.toString(modelDependencyManagement.size()) + "\n");
		List<DependencyID> toBeRemoved = new ArrayList<DependencyID>();
		for (Dependency dep : modelDependencyManagement) {
			DependencyID key = new DependencyID(dep);
			if (toBeFixed.containsKey(key)) {
				dep.setVersion(toBeFixed.get(key));
				getLog().info("Fixing: " + dep.getGroupId() + ":" + dep.getArtifactId() + " to: " + toBeFixed.get(key));
				Dependency d = dep;
				d.setVersion(toBeFixed.get(key));
				newDep.add(d);
				toBeRemoved.add(key);
			} else {
				newDep.add(dep);
			}
		}
		for (DependencyID d : toBeRemoved) {
			toBeFixed.remove(d);
		}
		for (DependencyID dID : toBeFixed.keySet()) {
			Dependency d = dID.toDependency();
			d.setVersion(toBeFixed.get(dID));
			newDep.add(d);
		}
		model.getDependencyManagement().setDependencies(newDep);
	}

	class DependencyID implements Comparable<DependencyID> {
		private String gID;
		private String aID;

		public DependencyID(Dependency dep) {
			gID = dep.getGroupId();
			aID = dep.getArtifactId();
		}

		public DependencyID(String groupId, String artifactId) {
			aID = artifactId;
			gID = groupId;
		}

		public int compareTo(DependencyID o) {
			int g = gID.compareTo(o.gID);
			if (g == 0) {
				return aID.compareTo(o.aID);
			}
			return g;
		}

		public String getGID() {
			return gID;
		}

		public String getAID() {
			return aID;
		}

		public Dependency toDependency() {
			Dependency dep = new Dependency();
			dep.setArtifactId(aID);
			dep.setGroupId(gID);
			return dep;
		}
	}

	public static List<MavenProject> getChildrenModules(MavenProject mavenProject, MavenProjectBuilder mpb,
			ArtifactRepository localRepository, ProfileManager pm) {
		List<MavenProject> children = new ArrayList<MavenProject>();
		List<String> modules = mavenProject.getModules();
		for (String mod : modules) {
			try {
				children.add(mpb.buildWithDependencies(new File(mavenProject.getBasedir(), mod + "/pom.xml"),
						localRepository, pm));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return children;
	}

	public static String replaceProperties(MavenProject mavenProject, String s) {
		String prop = s.replaceAll("\\$\\{(.*)\\}", "$1");
		return mavenProject.getProperties().getProperty(prop);
	}
}
