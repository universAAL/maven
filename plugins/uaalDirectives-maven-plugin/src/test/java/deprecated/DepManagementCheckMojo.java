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
package deprecated;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
   private Map<DependencyID, String> toBeFixed = new TreeMap<DependencyID, String>();
	
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
		String err;
		if (DirectiveCheckMojo.isRootProject(mavenProject)) {
			err = VERSIONS_NOT_CONFIGURED_ROOT;
			for (DependencyID dep : toBeFixed.keySet()) {
				err += "\n" + dep.getGID() + ":" + dep.getAID() 
						+ ", version should be : " + toBeFixed.get(dep) ;
			}
		}
		else {
			err = VERSIONS_NOT_CONFIGURED;
			for (DependencyID dep : toBeFixed.keySet()) {
				err += "\n" + dep.getGID() + ":" + dep.getAID() 
						+ ", version shouldn't be declared.";
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
			getLog().error(e);
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
		// check that the pom (not the model) hasn't any versions in it.
		List<Dependency> depMan = mavenProject2.getParent().getDependencyManagement().getDependencies();
		Map<DependencyID,String> depIDMan = new TreeMap<DepManagementCheckMojo.DependencyID, String>();
		// grather DependencyIDs form parent
		for (Dependency dep: depMan) {
			depIDMan.put(new DependencyID(dep), dep.getVersion());
		}
		try {
			for (Object o: PomWriter.readPOMFile(mavenProject2).getDependencies()) {
				Dependency dep = (Dependency) o;
				DependencyID depID = new DependencyID(dep);
				getLog().debug("***.1 " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion());
				getLog().debug("***.1 " + depID.getGID() + ":" + depID.getAID());
				if (depIDMan.containsKey(depID)
						&& dep.getVersion() != null) {
					getLog().debug("in DepManagement. Declared Version: " + dep.getVersion() + "Managed Version: " + depIDMan.get(depID));
					toBeFixed.put(depID, null);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toBeFixed.isEmpty();
	}

	private boolean passRootCheck(MavenProject mavenProject2) {
		Map<DependencyID,String> versionMap = getActualVersions(mavenProject2);
		List<Dependency> lod = mavenProject.getDependencyManagement().getDependencies();
		Map<DependencyID,String> lodVersionMap = new TreeMap<DependencyID,String>();
		
		// test if the version in DependencyManagement corresponds to the version of the actual artefact
		for (Dependency dependency : lod) {
			DependencyID depId = new DependencyID(dependency);
			String realVersion = versionMap.get(depId);
			lodVersionMap.put(depId, dependency.getVersion());
			getLog().debug("***1 ." + dependency.getGroupId() + ":" +  dependency.getArtifactId() + " Real:\"" + realVersion + "\" - Declared: \"" + dependency.getVersion()+"\"");
			if ( dependency != null
					&& !dependency.getVersion().equals(realVersion)
					&& realVersion != null) {
				getLog().debug("Marked as wrong.");
				toBeFixed.put( new DependencyID(dependency), realVersion);
			}
		}
		
		// test that every real artefact has an entry in the DependencyManagement
		for (DependencyID key : versionMap.keySet()) {
			if (!lodVersionMap.containsKey(key)) {
				toBeFixed.put(key, versionMap.get(key));
				getLog().debug("***2 ." + key.getGID() + ":" +  key.getAID() + " Not declared.");
				//System.out.println("***2 ." + key + ". - ." + versionMap.get(key) + ".");
			}
		}
		return toBeFixed.isEmpty();
	}

	private Map<DependencyID, String> getActualVersions(MavenProject mavenProject2) {
		TreeMap<DependencyID,String> versionMap = new TreeMap<DependencyID, String>();
		for (MavenProject mavenProject : reactorProjects) {
			if (mavenProject.getVersion() != null 
					&& !mavenProject.getPackaging().equals("pom")) {
				// Check if its a pom, add it if not!
				versionMap.put( new DependencyID(mavenProject.getGroupId(), mavenProject.getArtifactId())
						,mavenProject.getVersion());
				getLog().debug("added to ActualVersions: " + mavenProject.getGroupId() + ":" + mavenProject.getArtifactId()
						+ ":" + mavenProject.getVersion());
			}
		}
		return versionMap;
	}
	
	public void fix(Model model) {
		if (model.getPackaging().equals("pom")) {
			fixPOM(model);
		}
		else {
			fixNonPOM(model);
		}
	}

	public void fixNonPOM(Model model) {
		List<Dependency> ld = model.getDependencies();
		List<Dependency> nld = new ArrayList<Dependency>();
		for (Dependency dep: ld) {
			for (DependencyID depID : toBeFixed.keySet()){
				if (depID.compareTo(new DependencyID(dep)) == 0) {
					nld.add(depID.toDependency());
				}
				else {
					nld.add(dep);
				}
			}
		}
		model.setDependencies(nld);
	}
	
	public void fixPOM(Model model) {
		List<Dependency> modelDependencyManagement = model.getDependencyManagement().getDependencies();
		List<Dependency> newDep = new ArrayList<Dependency>();
		getLog().debug(Integer.toString(modelDependencyManagement.size())+"\n");
		List<DependencyID> toBeRemoved = new ArrayList<DepManagementCheckMojo.DependencyID>();
		for (Dependency dep : modelDependencyManagement) {
			DependencyID key  = new DependencyID(dep);
			if (toBeFixed.containsKey(key)) {
				dep.setVersion(toBeFixed.get(key));
				getLog().info("Fixing: " + dep.getGroupId() + ":" + dep.getArtifactId()
						+ " to: " + toBeFixed.get(key));
				Dependency d = key.toDependency();
				d.setVersion(toBeFixed.get(key));
				newDep.add(d);
				//toBeFixed.remove(key);
				toBeRemoved.add(key);
			}
			else {
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
	
	class DependencyID implements Comparable<DependencyID>{
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
			if (g == 0){
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
}
