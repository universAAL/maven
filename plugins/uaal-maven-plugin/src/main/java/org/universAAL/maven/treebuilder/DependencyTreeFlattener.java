package org.universAAL.maven.treebuilder;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.universAAL.maven.IndexingDependencyNodeVisitor;
import org.universAAL.maven.LaunchOrderDependencyNodeVisitor;
import org.universAAL.maven.ScopeArtifactFilter;

/**
 * This class is used for flattening dependency tree into an artifact list
 * sorted in the execution order.
 * 
 * @author rotgier
 * 
 */
public class DependencyTreeFlattener {

    private Log log;

    private ArtifactMetadataSource artifactMetadataSource;

    private ArtifactFactory artifactFactory;

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepository localRepository;

    private List remoteRepositories;

    private boolean throwExceptionOnConflict;

    public DependencyTreeFlattener(Log log,
	    ArtifactMetadataSource artifactMetadataSource,
	    ArtifactFactory artifactFactory,
	    MavenProjectBuilder mavenProjectBuilder,
	    ArtifactRepository localRepository, List remoteRepositories,
	    String throwExceptionOnConflictStr) {
	this.artifactMetadataSource = artifactMetadataSource;
	this.artifactFactory = artifactFactory;
	this.mavenProjectBuilder = mavenProjectBuilder;
	this.localRepository = localRepository;
	this.remoteRepositories = remoteRepositories;
	this.throwExceptionOnConflict = !("true"
		    .equals(throwExceptionOnConflictStr));
    }

    public String [] createArtifactExecutionList(String [] provisions, boolean transitive) throws MojoExecutionException, MojoFailureException {
	try {
	    Artifact pomArtifact = artifactFactory.createArtifact(
		    "org.universAAL.samples", "smp.lighting.client",
		    "1.0.1-SNAPSHOT", "", "pom");

	    MavenProject pomProject = mavenProjectBuilder.buildFromRepository(
		    pomArtifact, remoteRepositories, localRepository);

	    ArtifactFilter artifactFilter = new ScopeArtifactFilter();

	    DependencyTreeBuilder treeBuilder = new DependencyTreeBuilder(
		    artifactFactory, mavenProjectBuilder, localRepository,
		    remoteRepositories);

	    List rootNodes = treeBuilder.buildDependencyTree(localRepository,
		    artifactFactory, artifactMetadataSource, artifactFilter,
		    pomProject);

	    IndexingDependencyNodeVisitor filteringVisitor = new IndexingDependencyNodeVisitor(
		    log, artifactFactory, mavenProjectBuilder,
		    remoteRepositories, localRepository,
		    throwExceptionOnConflict);
	    LaunchOrderDependencyNodeVisitor visitor = new LaunchOrderDependencyNodeVisitor(
		    log, filteringVisitor.getNodesByArtifactId(),
		    filteringVisitor.getVersionByArtifactId(),
		    throwExceptionOnConflict, artifactFactory,
		    mavenProjectBuilder, remoteRepositories, localRepository);

	    Iterator rootNodesIterator = rootNodes.iterator();
	    int i = 0;
	    while (rootNodesIterator.hasNext()) {
		DependencyNode rootNode = (DependencyNode) rootNodesIterator
			.next();
		System.out.println("Rootnode: " + i);
		System.out.println(rootNode.toString());
	    }
	    rootNodesIterator = rootNodes.iterator();
	    while (rootNodesIterator.hasNext()) {
		DependencyNode rootNode = (DependencyNode) rootNodesIterator
			.next();
		rootNode.accept(filteringVisitor);
	    }
	    rootNodesIterator = rootNodes.iterator();
	    while (rootNodesIterator.hasNext()) {
		DependencyNode rootNode = (DependencyNode) rootNodesIterator
			.next();
		rootNode.accept(visitor);
	    }

	    List<DependencyNode> nodes = visitor.getNodes();
	    for (DependencyNode dependencyNode : nodes) {
		int state = dependencyNode.getState();
		Artifact artifact = dependencyNode.getArtifact();
		// if(state == DependencyNode.INCLUDED) {
		log.info("artifact: " + artifact.getArtifactId() + "-"
			+ artifact.getVersion());
		// }
	    }
	    
	    
	    return null;
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

}
