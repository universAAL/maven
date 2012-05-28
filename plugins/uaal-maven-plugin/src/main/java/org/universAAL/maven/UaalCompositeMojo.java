package org.universAAL.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
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
    protected ArtifactResolver artifactResolver;

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
    
    private static final String MAIN_COMPOSITE = "target/artifact.composite";
    
    private BufferedWriter createOutputWriter() throws FileNotFoundException {
	File targetDir = new File(baseDirectory, "target");
	targetDir.mkdirs();
	File generatedCompositeFile = new File(baseDirectory, MAIN_COMPOSITE);
	return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
		generatedCompositeFile, false)));
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
	try {
	    File manualArtifactComposite = new File(baseDirectory, "artifact.composite");
	    if (manualArtifactComposite.exists()) {
		getLog()
			.info(
				System.getProperty("line.separator")
					+ System.getProperty("line.separator")
					+ "Since artifact.composite exists in the base directory composite generation is abandoned."
					+ System.getProperty("line.separator")
					+ "Instead artifact.composite from basedir is simply copied to "
					+ MAIN_COMPOSITE
					+ System.getProperty("line.separator")
					+ System.getProperty("line.separator"));
		BufferedReader compositeReader = new BufferedReader(new InputStreamReader(
			new FileInputStream(manualArtifactComposite)));
		BufferedWriter compositeWriter = createOutputWriter();
		String line = null;
		while ((line = compositeReader.readLine())!= null) {
		    compositeWriter.write(line + System.getProperty("line.separator"));
		}
		compositeWriter.close();
		compositeReader.close();
	    } else {
//		if ("pom".equals(project.getArtifact().getType())) {
//		    getLog()
//			    .info(
//				    System.getProperty("line.separator")
//					    + System
//						    .getProperty("line.separator")
//					    + "Since this is a parent POM creating MAIN composite file is abandoned"
//					    + System
//						    .getProperty("line.separator")
//					    + System
//						    .getProperty("line.separator"));
//		} else {
		    getLog()
			    .info(
				    System.getProperty("line.separator")
					    + System
						    .getProperty("line.separator")
					    + "Creating MAIN composite file - output generated in "
					    + MAIN_COMPOSITE
					    + System
						    .getProperty("line.separator")
					    + System
						    .getProperty("line.separator"));
		    ExecutionListCreator execListCreator = new ExecutionListCreator(
			    getLog(), artifactMetadataSource, artifactFactory,
			    mavenProjectBuilder, localRepository,
			    remoteRepositories, artifactResolver,
			    throwExceptionOnConflictStr);
		    List mvnUrls = execListCreator
			    .createArtifactExecutionList(project);

		    BufferedWriter compositeWriter = createOutputWriter();
		    boolean hasWrittenSth = false;
		    for (Object mvnUrl : mvnUrls) {
			hasWrittenSth = true;
			String mvnUrlStr = (String) mvnUrl;
			compositeWriter.write("scan-bundle:" + mvnUrlStr
				+ System.getProperty("line.separator"));
		    }
		    if (!hasWrittenSth) {
			compositeWriter.write("This is an empty dummy line in order to make this file possible to deploy. Don't use this file at any time.");
		    }
		    compositeWriter.close();
		// }
	    }
	} catch (Exception e) {
	    getLog().error(e);
	    throw new RuntimeException(e);
	}
    }
}