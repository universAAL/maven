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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.util.PomFixer;
import org.universAAL.support.directives.util.PomWriter;
import org.universAAL.support.directives.util.SourceChecker;
import org.universAAL.support.directives.util.SourceExplorer;
import org.universAAL.support.directives.util.SourceFileReader;

/**
 * This checker will test whether the uAAL projects have itest dependency, and
 * if they have, then check for actual IntegrationTests. Fixes if necesary.
 * 
 * @author amedrano
 * 
 */
public class ItestsCheckFix implements APIFixableCheck, SourceChecker, PomFixer {

	static private String ITEST_MATCH = ".*org\\.universAAL\\.itests\\.IntegrationTest.*";
	
	//result of the check for implementations of IntegrationTests 
	private boolean integrationTestsPresent = false;

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenProject, Log log)
			throws MojoFailureException, MojoExecutionException {

		/*
		 * Check the Itest dependency
		 */
		List<Dependency> deps = mavenProject.getDependencies();
		boolean containsItests = false;
		while (deps.iterator().hasNext() && !containsItests) {
			Dependency d = (Dependency) deps.iterator().next();
			containsItests |= d.getArtifactId().equals("itests")
					&& d.getGroupId().equals("org.universAAL.support");
		}

		// if there are no dependencies then return
		if (!containsItests) {
			return true;
		}

		List<File> itest = findIntegrationTests(mavenProject.getBasedir());
		if (itest.size() == 0) {
			String m = "This project has a dependency to itests, but does not implement any IntegrationTest.\n"
					+ "Remove the itests dependency from the POM.";
			integrationTestsPresent  = false;
			throw new MojoFailureException(m);
		}
		integrationTestsPresent = true;
		return true;
	}

	public boolean passesTest(File f) {
		/*
		 * Check if any of the imports matches ITEST_MATCH
		 */
		ArrayList<String> imports = SourceFileReader.readImports(f);
		Iterator<String> I = imports.iterator();
		if (I.hasNext()) {
			String imp = I.next();
			while (I.hasNext() && !imp.matches(ITEST_MATCH)) {
				imp = I.next();
			}
			return imp.matches(ITEST_MATCH);
		} else {
			// If file has no imports then it passes
			return true;
		}
	}

	private List<File> findIntegrationTests(File basedir) {
		SourceExplorer se = new SourceExplorer(this);
		return se.walk(basedir + "/src/test/java/");
	}

	/** {@inheritDoc} */
	public void fix(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {

		try {
			new PomWriter(this, mavenProject).fix();
		} catch (Exception e) {
			log.error("unable to Write POM.");
			log.error(e);
		}
	}

	/** {@inheritDoc} */
	public void fix(Model model) {
		if (!integrationTestsPresent) {
			/*
			 * Remove the dependency to itests
			 */
			List<Dependency> deps = model.getDependencies();
			Dependency itestDep = null;
			while (deps.iterator().hasNext() && itestDep == null) {
				Dependency d = (Dependency) deps.iterator().next();
				if (d.getArtifactId().equals("itests")
						&& d.getGroupId().equals("org.universAAL.support")) {
					itestDep = d;
				}
			}
			if (itestDep != null) {
				model.removeDependency(itestDep);
			}
		}
	}

}
