package org.universAAL.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

public class LaunchOrderDependencyNodeVisitor extends FilteringVisitorSupport
	implements DependencyNodeVisitor {

    protected final List nodes = new ArrayList();

    /**
     * Mapping of stringified artifacts (groupId + artifactId + version) to
     * nodes in the dependency tree. Nodes contain information about children.
     * Duplicates of nodes are not included in the mapping.
     */
    private Map nodesByArtifactId = new HashMap();

    /**
     * Mapping of stringified artifacts (groupId + artifactId) to its version.
     */
    private Map versionsByArtifactId = new HashMap();

    protected void addNode(DependencyNode node) {
	try {
	    if (!wasVisited(node)) {
		if (1 == 2) {
		    File localRepoBaseDir = new File(localRepository
			    .getBasedir());
		    File jarPath = new File(localRepoBaseDir, localRepository
			    .pathOf(node.getArtifact()));
		    JarInputStream jio = new JarInputStream(
			    new FileInputStream(jarPath));
		    Manifest manifest = jio.getManifest();
		    Attributes attribs = manifest.getMainAttributes();
		    if (!attribs.containsKey("Bundle-ManifestVersion")) {
			// it means that the jar is not a bundle - it has to be
			// wrapped before installation in OSGi container

		    }
		}
		visited.add(stringify(node));
		nodes.add(node);
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    public LaunchOrderDependencyNodeVisitor(Log log, Map nodesByArtifactId,
	    Map versionsByArtifactId, boolean throwExceptionOnConflict,
	    ArtifactFactory artifactFactory,
	    MavenProjectBuilder mavenProjectBuilder, List remoteRepositories,
	    ArtifactRepository localRepository) {
	super(log, artifactFactory, mavenProjectBuilder, remoteRepositories,
		localRepository, throwExceptionOnConflict);
	this.nodesByArtifactId = nodesByArtifactId;
	this.versionsByArtifactId = versionsByArtifactId;
    }

    public boolean visit(DependencyNode node) {
	if (wasVisited(node)) {
	    return false;
	}
	if (isInScope(node)) {
	    switch (node.getState()) {
	    case DependencyNode.OMITTED_FOR_CONFLICT:
	    case DependencyNode.OMITTED_FOR_DUPLICATE:
		Artifact keptArtifact = node.getRelatedArtifact();
		if (keptArtifact == null) {
		    throw new RuntimeException(
			    "keptArtifact of ommited artifact is null: "
				    + node.getArtifact());
		}
		DependencyNode keptNode = (DependencyNode) nodesByArtifactId
			.get(stringify(keptArtifact));
		if (keptNode == null) {
		    /*
		     * keptNode can be null if dependency was conflicted and was
		     * overridden with other conflicting dependency e.g. having
		     * mw.data.representation in versions 0.3.1, 0.3.2,
		     * 1.0.0(resolved version) can impose that 0.3.1 is
		     * conflicted with 0.3.2
		     * 
		     * This can happen in both CONFLICT and DUPLICATE state. So
		     * to overcome this the versionsByArtifactId mapping is
		     * used.
		     */
		    keptNode = (DependencyNode) nodesByArtifactId
			    .get(versionsByArtifactId
				    .get(stringifyNoVersion(keptArtifact)));
		}
		if (keptNode == null) {
		    throw new IllegalStateException("Cannot find keptNode");
		}
		if (node.getState() == DependencyNode.OMITTED_FOR_CONFLICT) {
		    throwConflictException(node, keptNode,
			    "There is a conflict between kept dependency %s and omitted dependency %s");
		}

		keptNode.accept(this);
		return false;
	    }
	    return true;
	}
	return false;
    }

    public boolean endVisit(DependencyNode node) {
	if (!wasVisited(node)) {
	    if (isInScope(node)) {
		switch (node.getState()) {
		case DependencyNode.OMITTED_FOR_DUPLICATE:
		case DependencyNode.OMITTED_FOR_CONFLICT:
		    break;
		default:
		    addNode(node);
		}
	    }
	}
	return true;
    }

    public List getNodes() {
	return nodes;
    }
}
