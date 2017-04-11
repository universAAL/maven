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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.util.SourceChecker;
import org.universAAL.support.directives.util.SourceExplorer;
import org.universAAL.support.directives.util.SourceFileReader;

/**
 * This checker will test whether the uAAL projects are being properly decoupled
 * from OSGi.
 * 
 * @author amedrano
 *
 */
public class DecoupleCheck implements APICheck, SourceChecker {
	
	static private String OSGI_MATCH = ".*osgi.*";

	/** {@inheritDoc}  */
	public boolean check(MavenProject mavenProject, Log log) 
			throws MojoFailureException, MojoExecutionException {
			SourceExplorer se = new SourceExplorer(this);
			ArrayList<File> conflicted = se.walk(mavenProject.getBasedir()
					+ "/src/main/java/");
			if (conflicted.size() > 0) {
				String m = "The following Files are not Container Decoupled:\n";
				for (java.util.Iterator<File> iterator = conflicted.iterator(); iterator
						.hasNext();) {
					m += "\t" + iterator.next().getAbsolutePath() + "\n";
				}
				m += "To solve this problem, make sure there are no OSGi imports in your classes,"
						+ " unless the package that contains them has explicitly \"osgi\" in it's name.";
				throw new MojoFailureException(m);
			}
			return true;
		}

		public boolean passesTest(File f) {
			String pack = SourceFileReader.readPackage(f);
			if (!pack.matches(OSGI_MATCH)) {
				/*
				 * If package does not match OSGI_MATCH then check if any of the
				 * imports matches OSGI_MATCH
				 */
				ArrayList<String> imports = SourceFileReader.readImports(f);
				Iterator<String> I = imports.iterator();
				if (I.hasNext()) {
					String imp = I.next();
					while (I.hasNext() && !imp.matches(OSGI_MATCH)) {
						imp = I.next();
					}
					return !imp.matches(OSGI_MATCH);
				} else {
					// If file has no imports then it passes
					return true;
				}
			} else {
				// If the package name matches OSGI_MATCH then it passes
				return true;
			}

		}

}
