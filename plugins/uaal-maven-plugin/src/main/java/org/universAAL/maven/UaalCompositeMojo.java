package org.universAAL.maven;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.universAAL.maven.treebuilder.DependencyTreeBuilder;

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

    public void execute() throws MojoExecutionException, MojoFailureException {
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

	    boolean throwExceptionOnConflict = !("true"
		    .equals(throwExceptionOnConflictStr));

	    IndexingDependencyNodeVisitor filteringVisitor = new IndexingDependencyNodeVisitor(
		    getLog(), artifactFactory, mavenProjectBuilder,
		    remoteRepositories, localRepository,
		    throwExceptionOnConflict);
	    LaunchOrderDependencyNodeVisitor visitor = new LaunchOrderDependencyNodeVisitor(
		    getLog(), filteringVisitor.getNodesByArtifactId(),
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
		getLog().info(
			"artifact: " + artifact.getArtifactId() + "-"
				+ artifact.getVersion());
		// }
	    }
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

}