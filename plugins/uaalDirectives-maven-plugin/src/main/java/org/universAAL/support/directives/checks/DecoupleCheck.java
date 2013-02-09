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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.util.SourceChecker;
import org.universAAL.support.directives.util.SourceExplorer;

/**
 * This checker will test whether the uAAL projects are being properly decoupled
 * from OSGi.
 * 
 * @author amedrano
 *
 */
public class DecoupleCheck implements APICheck, SourceChecker {
	
	static private String OSGI_MATCH = ".*osgi.*";

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenProject, Log log)
			throws MojoExecutionException {
			SourceExplorer se = new SourceExplorer(this);
			ArrayList<File> conflicted = se.walk(mavenProject.getBasedir()
					+ "/src/main/java/");
			if (conflicted.size() > 0) {
				String m = System.getProperty("line.separator")
						+ System.getProperty("line.separator")
						+ "\nThe following Files are not Container Decoupled:\n";
				for (java.util.Iterator<File> iterator = conflicted.iterator(); iterator
						.hasNext();) {
					m += iterator.next().getAbsolutePath() + "\n";
				}
				m += System.getProperty("line.separator");
				m += "To solve this problem, make sure there are no OSGi imports in your classes,"
						+ " unless the package that contains them has explicitly \"osgi\" in it's name.";
				m += System.getProperty("line.separator")
				+ System.getProperty("line.separator");
				throw new MojoExecutionException(m);
			}
			return true;
		}

		public boolean passesTest(File f) {
			String pack = readPackage(f);
			if (!pack.matches(OSGI_MATCH)) {
				/*
				 * If package does not match OSGI_MATCH then check if any of the
				 * imports matches OSGI_MATCH
				 */
				ArrayList<String> imports = readImports(f);
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

		private static ArrayList<String> readImports(File f) {
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(f));
				return lookForLinesWith(br, ".*import.*");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		private static String readPackage(File f) {
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(f));
				ArrayList<String> r = lookForLinesWith(br, ".*package.*");
				if (r.size() > 0) {
					return r.get(0);
				} else {
					System.out.println("no package found for " + f.getName());
					System.out.flush();
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		private static ArrayList<String> lookForLinesWith(BufferedReader f, String regExp) {
			ArrayList<String> matches = new ArrayList<String>();
			String s;
			try {
				while ((s = f.readLine()) != null) {
					if (s.matches(regExp)) {
						matches.add(s);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return matches;
		}

}
