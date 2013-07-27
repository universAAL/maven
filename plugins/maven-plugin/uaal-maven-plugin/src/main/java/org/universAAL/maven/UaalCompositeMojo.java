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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
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
     * Directives configured via <configuration> in pom file, setting the
     * startlevel and/or nostart parameters to specified artifacts
     * 
     * @parameter
     */
    private StartSpec[] startArtifacts;

    /**
     * Default path to main composite.
     */
    private static final String MAIN_COMPOSITE = "target/artifact.composite";

    /**
     * Default path to deps composite.
     */
    private static final String MAIN_DEPS = "target/artifact.deps";

    /**
     * Creates output writer for given file name.
     * 
     * @param fileName
     *            for which writer will be created
     * @return output writer.
     * @throws FileNotFoundException
     *             when file does not exist
     */
    private BufferedWriter createOutputWriter(final String fileName)
	    throws FileNotFoundException {
	File targetDir = new File(baseDirectory, "target");
	targetDir.mkdirs();
	File generatedCompositeFile = new File(baseDirectory, fileName);
	return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
		generatedCompositeFile, false)));
    }

    /**
     * Writes given list of mvn urls to given file.
     * 
     * @param mvnUrls
     *            mvn urls
     * @param fileName
     *            file name
     * @throws IOException
     *             IOException
     */
    private void writeListToFile(final List mvnUrls, final String fileName)
	    throws IOException {
	BufferedWriter compositeWriter = createOutputWriter(fileName);
	boolean hasWrittenSth = false;
	for (Object mvnUrl : mvnUrls) {
	    hasWrittenSth = true;
	    String mvnUrlStr = (String) mvnUrl;
	    compositeWriter.write("scan-bundle:" + mvnUrlStr
		    + System.getProperty("line.separator"));
	}
	if (!hasWrittenSth) {
	    compositeWriter
		    .write("This is an empty dummy line in order to make"
			    + "this file possible to deploy."
			    + "Don't use this file at any time.");
	}
	compositeWriter.close();
    }

    /**
     * Execute.
     * 
     * @throws MojoExecutionException
     *             MojoExecutionException
     * @throws MojoFailureException
     *             MojoFailureException
     */
    public final void execute() throws MojoExecutionException,
	    MojoFailureException {
	try {
	    File manualArtifactComposite = new File(baseDirectory,
		    "artifact.composite");
	    String msg = System.getProperty("line.separator")
		    + System.getProperty("line.separator")
		    + "Since artifact.composite exists in"
		    + "the base directory composite generation is abandoned."
		    + System.getProperty("line.separator")
		    + "Instead artifact.composite from basedir"
		    + " is simply copied to " + MAIN_COMPOSITE
		    + System.getProperty("line.separator")
		    + System.getProperty("line.separator");
	    if (manualArtifactComposite.exists()) {
		getLog().info(msg);
		BufferedReader compositeReader = new BufferedReader(
			new InputStreamReader(new FileInputStream(
				manualArtifactComposite)));
		BufferedWriter compositeWriter = createOutputWriter(MAIN_COMPOSITE);
		String line = null;
		while ((line = compositeReader.readLine()) != null) {
		    compositeWriter.write(line
			    + System.getProperty("line.separator"));
		}
		compositeWriter.close();
		compositeReader.close();
	    } else {
		String msg2 = System.getProperty("line.separator")
			+ System.getProperty("line.separator")
			+ "Creating MAIN composite file - output generated in "
			+ MAIN_COMPOSITE + " and " + MAIN_DEPS
			+ System.getProperty("line.separator")
			+ System.getProperty("line.separator");
		getLog().info(msg2);
		ExecutionListCreator execListCreator = new ExecutionListCreator(
			getLog(), artifactMetadataSource, artifactFactory,
			mavenProjectBuilder, localRepository,
			remoteRepositories, artifactResolver,
			throwExceptionOnConflictStr, startArtifacts);
		List<String> mvnUrls = execListCreator
			.createArtifactExecutionList(project,
				new HashSet<String>(), false);
		writeListToFile(mvnUrls, MAIN_COMPOSITE);

		List<String> mvnUrlsOnlyDeps = new ArrayList<String>(mvnUrls);
		if (!mvnUrlsOnlyDeps.isEmpty()) {
		    mvnUrlsOnlyDeps.remove(mvnUrlsOnlyDeps.size() - 1);
		}
		writeListToFile(mvnUrlsOnlyDeps, MAIN_DEPS);

		getLog().info("");
		getLog().info(MAIN_COMPOSITE + ":");
		getLog().info("");
		int x = 1;
		for (String mvnUrl : mvnUrls) {
		    getLog().info(String.format("%2d. %s", x++, mvnUrl));
		}

		getLog().info("");
		getLog().info("");
		getLog().info(MAIN_DEPS + ":");
		getLog().info("");
		x = 1;
		for (String mvnUrl : mvnUrlsOnlyDeps) {
		    getLog().info(String.format("%2d. %s", x++, mvnUrl));
		}

	    }
	} catch (Exception e) {
	    getLog().error(e);
	    throw new RuntimeException(e);
	}
    }
}