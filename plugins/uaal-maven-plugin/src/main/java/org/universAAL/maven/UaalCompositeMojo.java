package org.universAAL.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.universAAL.maven.treebuilder.ExecutionListCreator;

/**
 * This mojo creates composite file (artifact.composite) for project in which it
 * is executed.
 * 
 * @goal composite
 */
public class UaalCompositeMojo extends AbstractMojo {

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
     * @parameter default-value="${basedir}"
     * @readonly
     * @required
     */
    private File baseDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
	try {
	    ExecutionListCreator execListCreator = new ExecutionListCreator(
		    getLog(), artifactMetadataSource, artifactFactory,
		    mavenProjectBuilder, localRepository, remoteRepositories,
		    throwExceptionOnConflictStr);
	    List mvnUrls = execListCreator.createArtifactExecutionList(project);
	    //File baseDirFile = new File(basedir);
	    File targetDir = new File(baseDirectory, "target");
	    File generatedCompositeFile = new File(targetDir,
		    "artifact.composite");
	    BufferedWriter compositeWriter = new BufferedWriter(
		    new OutputStreamWriter(new FileOutputStream(
			    generatedCompositeFile, false)));
	    for (Object mvnUrl : mvnUrls) {
		String mvnUrlStr = (String) mvnUrl;
		compositeWriter.write(mvnUrlStr
			+ System.getProperty("line.separator"));
	    }
	    compositeWriter.close();
	} catch (Exception e) {
	    getLog().error(e);
	    throw new RuntimeException(e);
	}
    }
}