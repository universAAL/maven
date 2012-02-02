package org.universAAL.maven.treebuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.universAAL.maven.IndexingDependencyNodeVisitor;
import org.universAAL.maven.LaunchOrderDependencyNodeVisitor;

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

    public ExecutionListCreator(Log log,
	    ArtifactMetadataSource artifactMetadataSource,
	    ArtifactFactory artifactFactory,
	    MavenProjectBuilder mavenProjectBuilder,
	    ArtifactRepository localRepository, List remoteRepositories,
	    ArtifactResolver artifactResolver,
	    String throwExceptionOnConflictStr) {
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
    }

    private class RootNode {

	public RootNode(DependencyNode rootNode, List remoteRepositories) {
	    this.rootNode = rootNode;
	    this.remoteRepositories = remoteRepositories;
	}

	private DependencyNode rootNode;
	private List remoteRepositories;
    }

    private List<ArtifactRepository> addMissingRepositories(
	    List<ArtifactRepository> remoteRepositories) {
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
     * @return a dependency tree as a list of rootnodes (instances of
     *         DependencyNode class) which contain their own subtrees.
     */
    private List<RootNode> parseProvisionsAndBuiltTree(String[] provisions,
	    boolean transitive, DependencyTreeBuilder treeBuilder)
	    throws Exception {
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
	    if (!provisionNoHeader.startsWith("mvn:")) {
		throw new IllegalArgumentException(
			"The URL "
				+ provision
				+ " does not start with \"mvn:\". Non mvn protocols are not supported");
	    }
	    provisionNoHeader = provisionNoHeader.substring("mvn:".length());
	    String[] provisionElements = provisionNoHeader.split("/");
	    if (provisionElements.length != 3) {
		throw new IllegalArgumentException(
			"The URL "
				+ provision
				+ "does not contain exactly two slashes \"/\". The URL is expected to provide groupId/artifactId/version");
	    }
	    Artifact pomArtifact = artifactFactory.createArtifact(
		    provisionElements[0], provisionElements[1],
		    provisionElements[2], "", "pom");
	    MavenProject pomProject = mavenProjectBuilder.buildFromRepository(
		    pomArtifact, remoteRepositories, localRepository);
	    List<ArtifactRepository> finalRemoteRepositories = addMissingRepositories(pomProject
		    .getRemoteArtifactRepositories());
	    projectDescs[i] = new MavenProjectDescriptor(pomProject,
		    finalRemoteRepositories, localtransitive);
	    listOfRemoteRepositories.add(finalRemoteRepositories);
	    i++;
	}
	List<DependencyNode> rootNodesOnly = treeBuilder.buildDependencyTree(
		localRepository, artifactFactory, artifactMetadataSource,
		projectDescs);
	Iterator<List> listOfRemoteRepositoriesIter = listOfRemoteRepositories
		.iterator();
	if (listOfRemoteRepositories.size() != rootNodesOnly.size()) {
	    throw new IllegalStateException(
		    "listOfRemoteRepositories.size() != rootNodesWithRepositories.size()");
	}
	List<RootNode> rootNodesWithRepositories = new ArrayList();
	for (DependencyNode rootNode : rootNodesOnly) {
	    rootNodesWithRepositories.add(new RootNode(rootNode,
		    listOfRemoteRepositoriesIter.next()));
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
     *            DependencyNode class) which contain their own subtrees.
     * @return execution list - list of strings representing mvnUrls of bundles
     *         which should be launched
     */
    private List processTreeIntoFlatList(List<RootNode> rootNodes) {
	Iterator<RootNode> rootNodesIterator = rootNodes.iterator();
	int i = 0;
	while (rootNodesIterator.hasNext()) {
	    RootNode rootNode = rootNodesIterator.next();
	    log.info("Dependency tree for artifact: "
		    + rootNode.rootNode.getArtifact()
		    + System.getProperty("line.separator")
		    + rootNode.toString());
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
		localRepository, artifactResolver);
	rootNodesIterator = rootNodes.iterator();
	while (rootNodesIterator.hasNext()) {
	    RootNode rootNode = rootNodesIterator.next();
	    visitor.setRemoteRepositories(rootNode.remoteRepositories);
	    rootNode.rootNode.accept(visitor);
	}

	List<String> mvnUrls = visitor.getMvnUrls();
	log.info("Final ordered execution list:");
	int x = 1;
	for (String mvnUrl : mvnUrls) {
	    log.info(String.format("%2d. %s", x++, mvnUrl));
	}

	return mvnUrls;
    }

    /**
     * Creates execution list for given MavenProject.
     * 
     * @param mavenProject
     * @return
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public List createArtifactExecutionList(MavenProject mavenProject)
	    throws Exception {
	DependencyTreeBuilder treeBuilder = new DependencyTreeBuilder(
		artifactFactory, mavenProjectBuilder, localRepository);
	List<ArtifactRepository> finalRemoteRpositories = addMissingRepositories(mavenProject
		.getRemoteArtifactRepositories());
	List<DependencyNode> rootNodes = treeBuilder.buildDependencyTree(
		localRepository, artifactFactory, artifactMetadataSource,
		new MavenProjectDescriptor(mavenProject,
			finalRemoteRpositories, true));
	if (rootNodes.size() != 1) {
	    throw new IllegalStateException("rootNodes.size() != 1");
	}
	List<RootNode> realRootNodes = new ArrayList<RootNode>();
	realRootNodes
		.add(new RootNode(rootNodes.get(0), finalRemoteRpositories));
	return processTreeIntoFlatList(realRootNodes);
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
    public List createArtifactExecutionList(String[] provisions,
	    boolean defaultTransitive) throws Exception {
	DependencyTreeBuilder treeBuilder = new DependencyTreeBuilder(
		artifactFactory, mavenProjectBuilder, localRepository);
	List<RootNode> rootNodes = parseProvisionsAndBuiltTree(provisions,
		defaultTransitive, treeBuilder);
	return processTreeIntoFlatList(rootNodes);
    }

}
