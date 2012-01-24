package org.universAAL.maven;

import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * This mojo creates composite file (artifact.composite) for project in which it
 * is executed.
 * 
 * @goal run
 */
public class UaalRunnerMojo extends AbstractMojo {
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
    private String [] provision;

    public void execute() throws MojoExecutionException, MojoFailureException {
	try {
	    MojoExecutor.plugin("org.ops4j", "maven-pax-plugin", "1.4");
	    MojoExecutor.goal("run");
	    //MojoExecutor.configuration("element)
	    //MojoExecutor.executeMojo(plugin, goal, configuration, env)
	    
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }
}
