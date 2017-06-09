/*
		Copyright 2011-2014 AGH-UST, http://www.agh.edu.pl
		Faculty of Computer Science, Electronics and Telecommunications
		Department of Computer Science 

        Copyright 2007-2014 CNR-ISTI, http://isti.cnr.it
        Institute of Information Science and Technologies
        of the Italian National Research Council
        
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
package org.universAAL.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.universAAL.itests.conf.IntegrationTestConsts;
import org.universAAL.maven.treebuilder.ExecutionListCreator;

/**
 * This mojo creates composite file (artifact-test.composite) for project in
 * which it is executed. Additionally it resolves artifact specified in
 * IntegrationTest.RUN_DIR_MVN_URL which is needed by integration tests.
 * 
 * @goal test
 * @author <a href="mailto:stefano.lenzi@isti.cnr.it">Stefano Lenzi</a>
 * @author <a href="marek.psiuk@agh.edu.pl">Marek Psiuk</a>
 * @version $LastChangedRevision$ ( $LastChangedDate$ )
 */
public class UaalTestMojo extends AbstractMojo {

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactFactory artifactFactory;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactResolver artifactResolver;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactMetadataSource artifactMetadataSource;

	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @parameter default-value="${ignore.dep.conflict}"
	 * @readonly
	 */
	private String throwExceptionOnConflictStr;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private MavenProjectBuilder mavenProjectBuilder;

	/**
	 * List of Remote Repositories used by the resolver.
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @readonly
	 * @required
	 */
	private List remoteRepositories;

	/**
	 * Location of the local repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @readonly
	 * @required
	 */
	private ArtifactRepository localRepository;

	/**
	 * @parameter default-value="${basedir}"
	 * @readonly
	 * @required
	 */
	private File baseDirectory;

	/**
	 * Set this to "true" to bypass unit tests entirely. You can also the
	 * skipTests parameter instead.
	 * 
	 * @since 1.3.2
	 * @parameter expression="${maven.test.skip}" defaultValue="false"
	 */
	private boolean skip;

	/**
	 * Set this to "true" to bypass unit tests entirely. You can also the
	 * skipTests parameter instead.
	 * 
	 * @since 1.3.2
	 * @parameter expression="${skipTests}" defaultValue="false"
	 */
	private boolean skipTests;

	/**
	 * Directives configured via <configuration> in pom file, setting the
	 * startlevel and/or nostart parameters to specified artifacts
	 * 
	 * @parameter
	 */
	private StartSpec[] startArtifacts;

	/**
	 * Execute.
	 * 
	 * @throws MojoExecutionException
	 *             MojoExecutionException
	 * @throws MojoFailureException
	 *             MojoFailureException
	 */
	public final void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if (skipTests()) {
				getLog().info("Creation of composite file for itests skipped");
				return;
			}
			if (!projectHasItestsDependency()) {
				getLog().info("Creation of composite file for itests skipped.\n"
						+ "There is no itests dependency detected, thus no need to generate test composite.");
				return;
			}
			if ("pom".equals(project.getArtifact().getType())) {
				getLog().info(System.getProperty("line.separator") + System.getProperty("line.separator")
						+ "Since this is a parent POM creating" + "composite file for itests is abandoned"
						+ System.getProperty("line.separator") + System.getProperty("line.separator"));
			} else {
				getLog().info(System.getProperty("line.separator") + System.getProperty("line.separator")
						+ "Creating composite file for itests " + "- output generated in "
						+ IntegrationTestConsts.TEST_COMPOSITE + System.getProperty("line.separator")
						+ System.getProperty("line.separator"));

				ExecutionListCreator execListCreator = new ExecutionListCreator(getLog(), artifactMetadataSource,
						artifactFactory, mavenProjectBuilder, localRepository, remoteRepositories, artifactResolver,
						throwExceptionOnConflictStr, startArtifacts);
				Set<String> separatedArtifactDepsOfRoot = new HashSet<String>();
				List<String> mvnUrls = execListCreator.createArtifactExecutionList(project, separatedArtifactDepsOfRoot,
						true, false);
				getLog().debug(IntegrationTestConsts.TEST_COMPOSITE + ":");
				int x = 1;
				for (String mvnUrl : mvnUrls) {
					getLog().debug(String.format("%2d. %s", x++, mvnUrl));
				}

				File targetDir = new File(baseDirectory, "target");
				targetDir.mkdirs();

				File generatedCompositeFile = new File(baseDirectory, IntegrationTestConsts.TEST_COMPOSITE);
				BufferedWriter compositeWriter = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(generatedCompositeFile, false)));
				for (Object mvnUrl : mvnUrls) {
					String mvnUrlStr = (String) mvnUrl;
					compositeWriter.write("scan-bundle:" + mvnUrlStr + System.getProperty("line.separator"));
				}
				compositeWriter.close();

				if (!separatedArtifactDepsOfRoot.isEmpty()) {
					File separatedArtifactDepsFile = new File(baseDirectory,
							IntegrationTestConsts.SEPARATED_ARTIFACT_DEPS);
					BufferedWriter separatedArtifactDepsWriter = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(separatedArtifactDepsFile, false)));
					for (String separatedArtifactMvnUrl : separatedArtifactDepsOfRoot) {
						separatedArtifactDepsWriter
								.write(separatedArtifactMvnUrl + System.getProperty("line.separator"));
					}
					separatedArtifactDepsWriter.close();
				}

				try {
					Artifact runDirArtifact = execListCreator
							.parseMvnUrlWithType(IntegrationTestConsts.getRunDirMvnUrl());
					artifactResolver.resolve(runDirArtifact, remoteRepositories, localRepository);
				} catch (Exception e) {
					getLog().warn("getRunDirMvnUrl (itests-rundir) could not be resolved", e);
				}
			}
		} catch (Exception e) {
			getLog().error(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @return true if either test has to be skipped
	 */
	private boolean skipTests() {
		return skip || skipTests;
	}

	private boolean projectHasItestsDependency() {
		/*
		 * Check the Itest dependency
		 */
		List<Dependency> deps = project.getDependencies();
		boolean containsItests = false;
		Iterator i = deps.iterator();
		while (i.hasNext() && !containsItests) {
			Dependency d = (Dependency) i.next();
			containsItests |= d.getArtifactId().equals("itests") && d.getGroupId().equals("org.universAAL.support");
		}
		return containsItests;
	}
}