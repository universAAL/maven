/*
	Copyright 2007-2014 Fraunhofer IGD, http://www.igd.fraunhofer.de
	Fraunhofer-Gesellschaft - Institute for Computer Graphics Research

	See the NOTICE file distributed with this work for additional
	information regarding copyright ownership

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	  http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package org.universAAL.support.maven.manifest;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;

/**
 * @goal uaalManifest
 */
public class MavenPlugin extends AbstractMojo {
	/**
	 * @parameter default-value=
	 *            "${project.basedir}/uaal-manifest.xml,${project.basedir}/target/uaal-manifest.xml"
	 */
	private File[] input;

	/**
	 * @parameter default-value= "${project.basedir}/target/uaal-manifest.mf"
	 */
	private File output;

	// /**
	// * @parameter default-value=
	// *
	// "${project.basedir}/target/classes/META-INF/MANIFEST.MF,${project.basedir}/target/test-classes/META-INF/MANIFEST.MF"
	// */
	// private File[] combine;

	public void setManifestPath(File path) {
		output = path;
	}

	public void setUaalManifestPath(File[] uaalPath) {
		input = uaalPath;
	}

	public void execute() {
		PermissionMap perms = new PermissionMap();

		for (File file : input) {
			if (file.exists()) {
				getLog().debug("Reading file '" + file + "'");
				ManifestReader reader = new ManifestReader(file);
				reader.read();
				PermissionMap res = reader.getResult();
				perms.add(res);
			}
		}

		if (perms.getPermissionCount() != 0) {
			getLog().debug("Writing to File '" + output + "'");
			ManifestWriter writer = new ManifestWriter(getLog(), output);
			writer.write(perms);
		}
		getLog().info("Found " + perms.toString());

		// if (output.exists()) {
		// for (File file : combine) {
		// getLog().debug("Combine file '" + file + "'");
		// ManifestCombine comb = new ManifestCombine(getLog(), output);
		// comb.combine(file);
		// }
		// }
	}

	// public static void main(String args[]) {
	// MavenPlugin p = new MavenPlugin();
	// p.setUaalManifestPath(new File[] { new File("uaal-manifest2.xml"),
	// new File("uaal-manifest3.xml") });
	// p.setManifestPath(new File("uaal-manifest.mf"));
	// p.execute();
	// }
}
