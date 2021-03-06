/*
Copyright 2011-2014 AGH-UST, http://www.agh.edu.pl
Faculty of Computer Science, Electronics and Telecommunications
Department of Computer Science

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

import static org.universAAL.maven.MyMojoExecutorV15.configuration;
import static org.universAAL.maven.MyMojoExecutorV15.element;
import static org.universAAL.maven.MyMojoExecutorV15.executeMojo;
import static org.universAAL.maven.MyMojoExecutorV15.goal;
import static org.universAAL.maven.MyMojoExecutorV15.name;
import static org.universAAL.maven.MyMojoExecutorV15.plugin;

import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.universAAL.maven.MyMojoExecutorV15.Element;
import org.universAAL.maven.treebuilder.ExecutionListCreator;

/**
 * This mojo creates composite file (artifact.composite) for project in which it
 * is executed.
 *
 * @goal run
 */
public class UaalRunnerMojo extends AbstractMojo implements Contextualizable {

	/**
	 * The Maven Project Object.
	 *
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The Maven Session Object.
	 *
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	private MavenSession session;

	/**
	 * The Maven PluginManager Object.
	 *
	 * @component
	 * @optional
	 */
	private PluginManager pluginManager;

	/**
	 * Artifact factory.
	 *
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactFactory artifactFactory;

	/**
	 * Artifact resolver.
	 *
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
	 * @parameter expression="${ignore.dep.conflict}" default-value="false"
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
	protected ArtifactRepository localRepository;

	/**
	 * @parameter expression="${run.args}"
	 */
	private String args;

	/**
	 * @parameter expression="${run.provision}"
	 */
	private String[] provision;

	/**
	 * @parameter expression="${run.transitive}" default-value="true"
	 * @optional
	 */
	private String transitive;

	/**
	 * @parameter expression="${separatedGroupIds}"
	 */
	private String[] separatedGroupIds;

	/**
	 * Directives configured via <configuration> in pom file, setting the
	 * startlevel and/or nostart parameters to specified artifacts
	 *
	 * @parameter
	 */
	private StartSpec[] startArtifacts;

	/**
	 * Plexus container.
	 */
	private PlexusContainer container;

	/**
	 * Contextualize.
	 *
	 * @param context
	 *            context
	 * @throws ContextException
	 *             ContextException
	 */
	public final void contextualize(final Context context) throws ContextException {
		container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
	}

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
			ExecutionListCreator execListCreator = new ExecutionListCreator(getLog(), artifactMetadataSource,
					artifactFactory, mavenProjectBuilder, localRepository, remoteRepositories, artifactResolver,
					throwExceptionOnConflictStr, startArtifacts);

			boolean defaultTransitive = true;
			if ("false".equals(transitive)) {
				defaultTransitive = false;
			}
			List mvnUrls = execListCreator.createArtifactExecutionList(provision, defaultTransitive, false);

			Element[] mvnUrlElems = new Element[mvnUrls.size()];
			int i = 0;
			for (Object mvnUrlObj : mvnUrls) {
				String mvnUrl = (String) mvnUrlObj;
				mvnUrlElems[i++] = new Element(name("provision"), mvnUrl);
			}
			try {
				executeMojo(plugin("org.ops4j", "maven-pax-plugin", "1.4"), goal("run"),
						configuration(element(name("args"), args), element(name("provision"), mvnUrlElems)),
						new MyMojoExecutorV15.ExecutionEnvironmentM2(project, session, pluginManager));
			} catch (Exception e) {
				Object buildPluginManager = container.lookup("org.apache.maven.plugin.BuildPluginManager");
				if (e.getCause() instanceof UnsupportedOperationException) {
					executeMojo(plugin("org.ops4j", "maven-pax-plugin", "1.4"), goal("run"),
							configuration(element(name("args"), args), element(name("provision"), mvnUrlElems)),
							new MyMojoExecutorV15.ExecutionEnvironmentM3(project, session, buildPluginManager));
				}
			}
		} catch (Exception e) {
			getLog().error(e);
			throw new RuntimeException(e);
		}
	}

}
