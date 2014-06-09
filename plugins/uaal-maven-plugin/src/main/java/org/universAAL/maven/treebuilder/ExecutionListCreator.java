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
package org.universAAL.maven.treebuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.universAAL.maven.IndexingDependencyNodeVisitor;
import org.universAAL.maven.LaunchOrderDependencyNodeVisitor;
import org.universAAL.maven.StartSpec;

/**
 * This class provides creation of OSGi bundles execution list on basis of list
 * of maven artifacts. Basically, if there is a need to launch given set of
 * maven artifacts (they assumed to be OSGi bundles) in OSGi congainer, the
 * class resolves all their dependencies and prepares execution list of bundles
 * (in a proper order) which have to be launched to have these maven artifacts
 * working.
 * 
 * It aggregates DependencyTreeBuilder and on top of it adds functionality of
 * flattening dependency tree into an artifact list sorted in the execution
 * order. All artifacts of execution list are resolved and downloaded to the
 * local maven repository.
 * 
 * @author rotgier
 * 
 */
public class ExecutionListCreator {

    private Log log;

    private ArtifactMetadataSource artifactMetadataSource;

    private ArtifactFactory artifactFactory;

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepository localRepository;

    private List remoteRepositories;

    private ArtifactResolver artifactResolver;

    private boolean throwExceptionOnConflict;

    /**
     * Directives configured via <configuration> in pom file, setting the
     * startlevel and/or nostart parameters to specified artifacts
     */
    private StartSpec[] startSpecs;

    public ExecutionListCreator(final Log log,
	    final ArtifactMetadataSource artifactMetadataSource,
	    final ArtifactFactory artifactFactory,
	    final MavenProjectBuilder mavenProjectBuilder,
	    final ArtifactRepository localRepository,
	    final List remoteRepositories,
	    final ArtifactResolver artifactResolver,
	    final String throwExceptionOnConflictStr,
	    final StartSpec[] startSpecs) {
	this.log = log;
	this.artifactMetadataSource = artifactMetadataSource;
	this.artifactFactory = artifactFactory;
	this.mavenProjectBuilder = mavenProjectBuilder;
	this.localRepository = localRepository;
	this.remoteRepositories = remoteRepositories;
	this.artifactFactory = artifactFactory;
	this.artifactResolver = artifactResolver;
	this.throwExceptionOnConflict = !("true"
		.equals(throwExceptionOnConflictStr));
	this.startSpecs = startSpecs;
    }

    /**
     * Method verifies if passed list of repositories contains paxrunner and
     * ops4j-releases repositories. If not, a new list containing passed
     * repositories plus paxrunner and ops4j-releases is created and returned.
     * 
     * @param remoteRepositories
     * @return
     */
    private List<ArtifactRepository> addMissingRepositories(
	    final List<ArtifactRepository> remoteRepositories) {
	List<ArtifactRepository> modifiedRemoteRepositories = new ArrayList<ArtifactRepository>(
		remoteRepositories);
	boolean paxRunnerPresent = false;
	boolean ops4jPresent = false;
	for (Object repoObj : modifiedRemoteRepositories) {
	    ArtifactRepository repo = (ArtifactRepository) repoObj;
	    if ("paxrunner".equals(repo.getId())) {
		paxRunnerPresent = true;
	    }
	    if ("ops4j-releases".equals(repo.getId())) {
		ops4jPresent = true;
	    }
	}
	if (!paxRunnerPresent) {
	    ArtifactRepository repo = new DefaultArtifactRepository(
		    "paxrunner",
		    "http://osgi.sonatype.org/content/groups/pax-runner",
		    new DefaultRepositoryLayout(),
		    new ArtifactRepositoryPolicy(false, null, null),
		    new ArtifactRepositoryPolicy());
	    modifiedRemoteRepositories.add(repo);
	}
	if (!ops4jPresent) {
	    ArtifactRepository repo = new DefaultArtifactRepository(
		    "ops4j-releases", "http://repository.ops4j.org/maven2",
		    new DefaultRepositoryLayout(),
		    new ArtifactRepositoryPolicy(false, null, null),
		    new ArtifactRepositoryPolicy());
	    modifiedRemoteRepositories.add(repo);
	}
	return modifiedRemoteRepositories;
    }

    /**
     * Method builds dependency tree for a list of provision strings and
     * parameter specifying default transitiveness. Method validates format of
     * provision strings and parses them into representation acceptable by
     * DependencyTreeBuilder. DependencyTreeBuilder is invoked and its output is
     * returned.
     * 
     * @return a dependency tree as a list of rootnodes (instances of RootNode
     *         class) which contain their own subtrees.
     */
    private List<RootNode> parseProvisionsAndBuiltTree(
	    final String[] provisions, final boolean transitive,
	    final DependencyTreeBuilder treeBuilder) throws Exception {
	MavenProjectDescriptor[] projectDescs = new MavenProjectDescriptor[provisions.length];
	int i = 0;
	List<List> listOfRemoteRepositories = new ArrayList<List>();
	for (String provision : provisions) {
	    boolean localtransitive = transitive;
	    String provisionNoHeader = provision;
	    if (provision.startsWith("transitive:")) {
		provisionNoHeader = provision.substring("transitive:".length());
		localtransitive = true;
	    } else if (provision.startsWith("nontransitive:")) {
		provisionNoHeader = provision.substring("nontransitive:"
			.length());
		localtransitive = false;
	    }
	    Artifact pomArtifact = parseMvnUrl(provisionNoHeader);
	    MavenProject pomProject = mavenProjectBuilder.buildFromRepository(
		    pomArtifact, remoteRepositories, localRepository);
	    List<ArtifactRepository> finalRemoteRepositories = addMissingRepositories(pomProject
		    .getRemoteArtifactRepositories());
	    projectDescs[i] = new MavenProjectDescriptor(pomProject,
		    finalRemoteRepositories, localtransitive);
	    listOfRemoteRepositories.add(finalRemoteRepositories);
	    i++;
	}
	List<RootNode> rootNodesOnly = treeBuilder.buildDependencyTree(
		localRepository, artifactFactory, artifactMetadataSource,
		projectDescs);
	Iterator<List> listOfRemoteRepositoriesIter = listOfRemoteRepositories
		.iterator();
	if (listOfRemoteRepositories.size() != rootNodesOnly.size()) {
	    throw new IllegalStateException(
		    "listOfRemoteRepositories.size() != rootNodesWithRepositories.size()");
	}
	List<RootNode> rootNodesWithRepositories = new ArrayList();
	for (RootNode rootNode : rootNodesOnly) {
	    rootNode.remoteRepositories = listOfRemoteRepositoriesIter.next();
	    rootNodesWithRepositories.add(rootNode);
	}
	return rootNodesWithRepositories;
    }

    /**
     * Method flattens provided dependency tree and creates on its basis an
     * execution list. To do this, method walks the tree twice. At first all
     * artifacts are indexed. In second walk artifacts are processed in a
     * deep-first manner. When an omitted artifact is encountered, walk is
     * continued at related kept artifact. After finishing processing of all
     * childs of given artifact, the artifact is added to the execution list.
     * Thanks to that it is ensured that before bundle will be started, all
     * dependency bundles will be started earlier.
     * 
     * @param rootNodes
     *            a dependency tree as a list of rootnodes (instances of
     *            RootNode class) which contain their own subtrees and lists of
     *            remote repositories.
     * @param dontResolve
     *            artifact which should not be resolved
     * @return execution list - list of strings representing mvnUrls of bundles
     *         which should be launched
     */
    private List processTreeIntoFlatList(final List<RootNode> rootNodes,
	    final Artifact dontResolve) {
	Iterator<RootNode> rootNodesIterator = rootNodes.iterator();
	while (rootNodesIterator.hasNext()) {
	    RootNode rootNode = rootNodesIterator.next();
	    log.debug("Dependency tree for artifact: "
		    + rootNode.rootNode.getArtifact()
		    + System.getProperty("line.separator")
		    + rootNode.rootNode.toString());
	}

	IndexingDependencyNodeVisitor filteringVisitor = new IndexingDependencyNodeVisitor(
		log);
	rootNodesIterator = rootNodes.iterator();
	while (rootNodesIterator.hasNext()) {
	    RootNode rootNode = rootNodesIterator.next();
	    rootNode.rootNode.accept(filteringVisitor);
	}

	LaunchOrderDependencyNodeVisitor visitor = new LaunchOrderDependencyNodeVisitor(
		log, filteringVisitor.getNodesByArtifactId(), filteringVisitor
			.getVersionByArtifactId(), throwExceptionOnConflict,
		localRepository, artifactResolver, dontResolve, startSpecs);
	rootNodesIterator = rootNodes.iterator();
	while (rootNodesIterator.hasNext()) {
	    RootNode rootNode = rootNodesIterator.next();
	    visitor.setExcludedCoreArtifacts(rootNode.excludedCoreArtifacts);
	    rootNode.rootNode.accept(visitor);
	}

	List<String> mvnUrls = visitor.getMvnUrls();
	return mvnUrls;
    }

    /**
     * Parses mvn url which has to be in the following format:
     * mvn:/groupId/artifactId/version.
     * 
     * @param mvnurl
     * @return returns maven artifact which corresponds to the provided mvnurl.
     */
    private Artifact parseMvnUrl(String mvnurl) {
	if (!mvnurl.startsWith("mvn:")) {
	    throw new IllegalArgumentException(
		    "The URL "
			    + mvnurl
			    + " does not start with \"mvn:\". Non mvn protocols are not supported");
	}
	mvnurl = mvnurl.substring("mvn:".length());
	String[] provisionElements = mvnurl.split("/");
	if (provisionElements.length != 3) {
	    throw new IllegalArgumentException(
		    "The URL "
			    + mvnurl
			    + "does not contain exactly two slashes \"/\". The URL is expected to provide groupId/artifactId/version");
	}
	Artifact pomArtifact = artifactFactory.createArtifact(
		provisionElements[0], provisionElements[1],
		provisionElements[2], "", "pom");
	return pomArtifact;
    }

    /**
     * Parses mvn url which has to be in the following format:
     * mvn:/groupId/artifactId/version/type.
     * 
     * @param mvnurl
     * @return returns maven artifact which corresponds to the provided mvnurl.
     */
    public Artifact parseMvnUrlWithType(String mvnurl) {
	if (!mvnurl.startsWith("mvn:")) {
	    throw new IllegalArgumentException(
		    "The URL "
			    + mvnurl
			    + " does not start with \"mvn:\". Non mvn protocols are not supported");
	}
	mvnurl = mvnurl.substring("mvn:".length());
	String[] provisionElements = mvnurl.split("/");
	Artifact pomArtifact = null;
	if (provisionElements.length == 3) {
	    pomArtifact = artifactFactory.createArtifact(provisionElements[0],
		    provisionElements[1], provisionElements[2], "", "jar");
	} else if (provisionElements.length != 4) {
	    throw new IllegalArgumentException(
		    "The URL "
			    + mvnurl
			    + " does not contain exactly three slashes \"/\". The URL is expected to provide groupId/artifactId/version/type");
	} else {
	    pomArtifact = artifactFactory.createArtifact(provisionElements[0],
		    provisionElements[1], provisionElements[2], "",
		    provisionElements[3]);
	}
	return pomArtifact;
    }

    /**
     * Creates execution list for given MavenProject.
     * 
     * @param mavenProject
     * @return
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public List createArtifactExecutionList(final MavenProject mavenProject,
	    final Set<String> separatedArtifactDepsOfRootMvnUrls,
	    final boolean includeTestRuntimes) throws Exception {
	DependencyTreeBuilder treeBuilder = new DependencyTreeBuilder(
		artifactFactory, mavenProjectBuilder, localRepository,
		includeTestRuntimes);
	List<ArtifactRepository> finalRemoteRpositories = addMissingRepositories(mavenProject
		.getRemoteArtifactRepositories());
	List<RootNode> rootNodes = treeBuilder.buildDependencyTree(
		localRepository, artifactFactory, artifactMetadataSource,
		new MavenProjectDescriptor(mavenProject,
			finalRemoteRpositories, true));
	List<ResolutionNode> separatedArtifactDepsOfRoot = treeBuilder
		.getSeparatedArtifactDepsOfRoot();
	for (ResolutionNode separatedRootDep : separatedArtifactDepsOfRoot) {
	    Artifact artifact = separatedRootDep.getArtifact();
	    separatedArtifactDepsOfRootMvnUrls.add(String.format(
		    "mvn:%s/%s/%s", artifact.getGroupId(), artifact
			    .getArtifactId(), artifact.getVersion()));
	}
	if (rootNodes.size() != 1) {
	    throw new IllegalStateException("rootNodes.size() != 1");
	}
	List<RootNode> realRootNodes = new ArrayList<RootNode>();
	RootNode theRootNode = rootNodes.get(0);
	theRootNode.remoteRepositories = finalRemoteRpositories;
	realRootNodes.add(theRootNode);
	return processTreeIntoFlatList(realRootNodes, mavenProject
		.getArtifact());
    }

    /**
     * Creates execution list on basis of provisions list and parameter
     * specifying default transitiveness.
     * 
     * @param provisions
     *            list of provision strings. Each provision string has to have
     *            the following format:
     *            (nontransitive|transitive)?mvn:/groupId/artifactId/version.
     *            Information at the beginning: "transitive", "nontransitive" is
     *            optional. If it is present it overrides the defaultTransitive
     *            parameter but only for given provision string.
     * @param defaultTransitive
     *            default value of transitive parameter for all provision
     *            strings. If transitive is true then artifact related to each
     *            provision string is recursively resolved and all its regular
     *            and uAAL-Runtime maven dependencies are included in returned
     *            artifact list. If transitive is false then only artifacts
     *            (related to provision strings) themselves are resolved without
     *            looking at their dependencies. Thanks to that, providing
     *            explicit list of artifact to be launched is possible.
     * @return execution list - list of strings representing mvnUrls of bundles
     *         which should be launched
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public List createArtifactExecutionList(final String[] provisions,
	    final boolean defaultTransitive, final boolean includeTestRuntimes)
	    throws Exception {
	DependencyTreeBuilder treeBuilder = new DependencyTreeBuilder(
		artifactFactory, mavenProjectBuilder, localRepository,
		includeTestRuntimes);
	List<RootNode> rootNodes = parseProvisionsAndBuiltTree(provisions,
		defaultTransitive, treeBuilder);
	return processTreeIntoFlatList(rootNodes, null);
    }

}
