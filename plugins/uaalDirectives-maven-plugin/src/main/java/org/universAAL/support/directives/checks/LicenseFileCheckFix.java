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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarInputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APIFixableCheck;

/**
 * Check that ASL2.0.txt and NOTICE.txt files Exists. Also checks that
 * LICENSE.txt does not exist.
 * 
 * @author amedrano
 *
 */
public class LicenseFileCheckFix implements APIFixableCheck {

	private static final String[] files = { "ASL2.0.txt",
			// "MITX.txt",
			"NOTICE.txt", // NOTICE should always be last!
	};

	private static final String[] exFiles = { "LICENSE.txt", "LICENCE.txt", };

	private static final String NOT_FOUND = "License File does not exist: ";

	private static final String FOUND = "License File exist (where it should not): ";

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenproject, Log log) throws MojoExecutionException, MojoFailureException {
		String message = "";
		if (mavenproject.getPackaging().equals("pom")) {
			return true;
		}
		for (int i = 0; i < files.length; i++) {
			if (!new File(mavenproject.getBasedir(), files[i]).exists()) {
				message += NOT_FOUND + files[i] + "\n";
			}
		}
		for (int i = 0; i < exFiles.length; i++) {
			if (new File(mavenproject.getBasedir(), exFiles[i]).exists()) {
				message += FOUND + exFiles[i] + "\n";
			}
		}
		if (!message.isEmpty()) {
			throw new MojoFailureException(message);
		}
		return true;
	}

	/** {@inheritDoc} */
	public void fix(MavenProject mavenProject, Log log) throws MojoExecutionException, MojoFailureException {
		// does not try NOTICE.txt!
		for (int i = 0; i < files.length - 1; i++) {
			File lf = new File(mavenProject.getBasedir(), files[i]);
			if (!lf.exists()) {
				try {
					copyFile(files[i], lf);
				} catch (IOException e) {
				}
			}
		}
		if (!new File(mavenProject.getBasedir(), files[files.length - 1]).exists()) {
			generateNoticeFile(mavenProject);
		}
		for (int i = 0; i < exFiles.length; i++) {
			File lf = new File(mavenProject.getBasedir(), exFiles[i]);
			if (lf.exists()) {
				lf.delete();
			}
		}
	}

	/**
	 * @param mavenProject
	 */
	private void generateNoticeFile(MavenProject mavenProject) {
		File notice = new File(mavenProject.getBasedir(), files[files.length - 1]);
		// TODO use mavenProject.getDependencies() to generate notice.
	}

	private void copyFile(String sourceName, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		} else {
			throw new IOException("Destination File: " + destFile.getName() + " exists.");
		}

		JarInputStream is = new JarInputStream(getClass().getResourceAsStream(sourceName));
		FileOutputStream os = new FileOutputStream(destFile);

		try {
			int c;
			while ((c = is.read()) != -1) {
				os.write(c);
			}
		} finally {
			if (is != null) {
				is.close();
			}
			if (os != null) {
				os.close();
			}
		}
	}

}
