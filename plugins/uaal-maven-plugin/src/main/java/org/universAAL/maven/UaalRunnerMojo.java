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
     * The Maven Project Object
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The Maven Session Object
     * 
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * The Maven PluginManager Object
     * 
     * @component
     * @optional
     */
    protected PluginManager pluginManager;

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
    protected ArtifactResolver artifactResolver;

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
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List remoteRepositories;

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

    private PlexusContainer container;

    public void contextualize(final Context context) throws ContextException {
	container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
	try {
	    ExecutionListCreator execListCreator = new ExecutionListCreator(
		    getLog(), artifactMetadataSource, artifactFactory,
		    mavenProjectBuilder, localRepository, remoteRepositories,
		    artifactResolver, throwExceptionOnConflictStr);

	    boolean defaultTransitive = true;
	    if ("false".equals(transitive)) {
		defaultTransitive = false;
	    }
	    List mvnUrls = execListCreator.createArtifactExecutionList(
		    provision, defaultTransitive, false);

	    Element[] mvnUrlElems = new Element[mvnUrls.size()];
	    int i = 0;
	    for (Object mvnUrlObj : mvnUrls) {
		String mvnUrl = (String) mvnUrlObj;
		mvnUrlElems[i++] = new Element(name("provision"), mvnUrl);
	    }
	    try {
		executeMojo(plugin("org.ops4j", "maven-pax-plugin", "1.4"),
			goal("run"), configuration(element(name("args"), args),
				element(name("provision"), mvnUrlElems)),
			new MyMojoExecutorV15.ExecutionEnvironmentM2(project,
				session, pluginManager));
	    } catch (Exception e) {
		Object buildPluginManager = container
			.lookup("org.apache.maven.plugin.BuildPluginManager");
		if (e.getCause() instanceof UnsupportedOperationException) {
		    executeMojo(plugin("org.ops4j", "maven-pax-plugin", "1.4"),
			    goal("run"), configuration(element(name("args"),
				    args), element(name("provision"),
				    mvnUrlElems)),
			    new MyMojoExecutorV15.ExecutionEnvironmentM3(
				    project, session, buildPluginManager));
		}
	    }
	} catch (Exception e) {
	    getLog().error(e);
	    throw new RuntimeException(e);
	}
    }

}
