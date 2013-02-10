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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APICheck;

/**
 * @author amedrano
 *
 */
public class MavenCoordinateCheck implements APICheck {

	private String artifactIdMatchString;
	private String groupIdMatchString;
	private String versionMatchString;
	private String nameMatchString;



	public MavenCoordinateCheck(String artifactIdMatchString,
			String groupIdMatchString, String versionMatchString,
			String nameMatchString) {
		super();
		this.artifactIdMatchString = artifactIdMatchString;
		this.groupIdMatchString = groupIdMatchString;
		this.nameMatchString = nameMatchString;
		this.versionMatchString = versionMatchString;
	}



	/** {@inheritDoc} */
	public boolean check(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {

		Pattern pAId = Pattern.compile(artifactIdMatchString);
		Pattern pGId = Pattern.compile(groupIdMatchString);
		Pattern pVer = Pattern.compile(versionMatchString);
		Pattern pNam = Pattern.compile(nameMatchString);

		Matcher mAId = pAId.matcher(mavenProject.getModel().getArtifactId());
		Matcher mGId = pGId.matcher(mavenProject.getModel().getGroupId());
		Matcher mVer = pVer.matcher(mavenProject.getModel().getVersion());
		Matcher mNam = pNam.matcher(mavenProject.getModel().getName());

		StringBuffer message = new StringBuffer();

		if (!mAId.find()) {
			message.append("ArtifactId: " + mavenProject.getArtifactId() 
					+ "\n does not match convention: " 
					+ artifactIdMatchString + "\n");
		}
		if (!mGId.find()) {
			message.append("GroupId: " + mavenProject.getGroupId() 
					+ "\n does not match convention: " 
					+ groupIdMatchString + "\n");
		}
		if (!mVer.find()) {
			message.append("Version: " + mavenProject.getVersion() 
					+ "\n does not match convention: " 
					+ versionMatchString + "\n");
		}
		if (!mNam.find()) {
			message.append("Artifact Name: " + mavenProject.getName()
					+ "\n does not match convention: " 
					+ nameMatchString + "\n");
		}

		if (message.length() > 0) {
			throw new MojoFailureException(message.toString());
		}
		
		return true;
	}
}