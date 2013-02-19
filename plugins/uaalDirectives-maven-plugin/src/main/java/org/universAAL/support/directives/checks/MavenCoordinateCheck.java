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

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.util.PomWriter;

/**
 * @author amedrano
 *
 */
public class MavenCoordinateCheck implements APICheck {

	private static final String VERSION_MATCH_PROP = "versionMatchString";
	private static final String NAME_MATCH_PROP = "nameMatchString";
	private static final String GROUP_ID_MATCH_PROP = "groupIdMatchString";
	private static final String ARTIFACT_ID_MATCH_PROP = "artifactIdMatchString";
	private static final String DEFAULT_MATCH = ".*";
	
	private static final String DOES_NOT_MATCH_CONVENTION = "\ndoes not match convention: ";

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {

		String artifactIdMatchString = 
				mavenProject.getProperties().getProperty(ARTIFACT_ID_MATCH_PROP, DEFAULT_MATCH);
		String groupIdMatchString = 
				mavenProject.getProperties().getProperty(GROUP_ID_MATCH_PROP, DEFAULT_MATCH);
		String nameMatchString = 
				mavenProject.getProperties().getProperty(NAME_MATCH_PROP, DEFAULT_MATCH);
		String versionMatchString = 
				mavenProject.getProperties().getProperty(VERSION_MATCH_PROP, DEFAULT_MATCH);
		
		Pattern pAId = Pattern.compile(artifactIdMatchString);
		Pattern pGId = Pattern.compile(groupIdMatchString);
		Pattern pVer = Pattern.compile(versionMatchString);
		Pattern pNam = Pattern.compile(nameMatchString);

		Matcher mAId = pAId.matcher(mavenProject.getArtifactId());
		Matcher mGId = pGId.matcher(mavenProject.getGroupId());
		Matcher mVer = pVer.matcher(mavenProject.getVersion());
		Matcher mNam = pNam.matcher(mavenProject.getName());

		StringBuffer message = new StringBuffer();

		if (!mAId.find()) {
			message.append("ArtifactId: " + mavenProject.getArtifactId() 
					+ DOES_NOT_MATCH_CONVENTION 
					+ artifactIdMatchString + "\n");
		}
		if (!mGId.find()) {
			message.append("GroupId: " + mavenProject.getGroupId() 
					+ DOES_NOT_MATCH_CONVENTION 
					+ groupIdMatchString + "\n");
		}
		if (!mVer.find()) {
			message.append("Version: " + mavenProject.getVersion() 
					+ DOES_NOT_MATCH_CONVENTION 
					+ versionMatchString + "\n");
		}
		if (!mNam.find()) {
			message.append("Artifact Name: " + mavenProject.getName()
					+ DOES_NOT_MATCH_CONVENTION 
					+ nameMatchString + "\n");
		}

		if (message.length() > 0) {
			throw new MojoFailureException(message.toString());
		}
		Model pomFileModel = null;
		try {
			pomFileModel = PomWriter.readPOMFile(mavenProject);
		} catch (Exception e) {
		}
		
		if (!mavenProject.getPackaging().equals("pom")
				&& pomFileModel != null
				&& (pomFileModel.getProperties().containsKey(ARTIFACT_ID_MATCH_PROP)
						|| pomFileModel.getProperties().containsKey(GROUP_ID_MATCH_PROP)
						|| pomFileModel.getProperties().containsKey(VERSION_MATCH_PROP)
						|| pomFileModel.getProperties().containsKey(NAME_MATCH_PROP))){
			throw new MojoFailureException("This project has declared naming conventions when it shoudln't.\n"
					+ "This is probably an attempt to skip this directive, SHAME ON YOU!");
		}
	
		
		return true;
	}
}