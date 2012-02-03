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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * This DepepdencyVistor traverses depedency tree in depth-first manner. Visitor
 * needs two collections: nodesByArtifactId and versionsByArtifactId which map
 * respective stringified artifact representations to DependencyNodes. If
 * visitor encounters node which was omitted, then it checks the reason for
 * omitting. If it was omitted because of duplication then visitor looks up the
 * kept node and continues traversing from it. If it was omitted because of
 * conflict then an exception with meaningful message is thrown, unless the
 * visitor is instructed to not throw exception by passing
 * throwExceptionOnConflict parameter to the constructor.
 * 
 * After ending a visit to node which was not ommitted the node's artifact is
 * converted to mvn url and aded to mvnUrls list. If the artifact is not a
 * bundle then mvn url is preceded with "wrap:" protocol. After visit is ended
 * all mvnUrls in required launch order are contained in mvnUrl list.
 * 
 * @author rotgier
 * 
 */
public class LaunchOrderDependencyNodeVisitor extends FilteringVisitorSupport
	implements DependencyNodeVisitor {

    private ArtifactRepository localRepository;

    private List remoteRepositories;

    private ArtifactResolver artifactResolver;

    /**
     * Stringified representation of artifact which should not be resolved.
     */
    private String artifactDontResolve;

    /**
     * After finishing the visit this list contains mvnUrls in launch order of
     * all artifacts needed for processed run configuration.
     */
    private final List mvnUrls = new ArrayList();

    /**
     * Mapping of stringified artifacts (groupId:artifactId:version) to nodes in
     * the dependency tree. Nodes contain information about children. Duplicates
     * of nodes are not included in the mapping.
     */
    private Map nodesByArtifactId = new HashMap();

    /**
     * Mapping of stringified artifacts (groupId:artifactId) to its version.
     */
    private Map versionsByArtifactId = new HashMap();

    /**
     * Whether exception should be thrown in case of conflict in artifact
     * version.
     */
    private boolean throwExceptionOnConflict = true;

    public LaunchOrderDependencyNodeVisitor(Log log, Map nodesByArtifactId,
	    Map versionsByArtifactId, boolean throwExceptionOnConflict,
	    ArtifactRepository localRepository,
	    ArtifactResolver artifactResolver, Artifact dontResolve) {
	super(log);
	this.localRepository = localRepository;
	this.nodesByArtifactId = nodesByArtifactId;
	this.versionsByArtifactId = versionsByArtifactId;
	this.throwExceptionOnConflict = throwExceptionOnConflict;
	this.artifactResolver = artifactResolver;
	if (dontResolve != null) {
	    this.artifactDontResolve = stringify(dontResolve);
	}
    }

    /**
     * If throwExceptionOnConflict is true, this method throws exception about
     * conflict between omitted and kept node.
     * 
     * @param omittedNode
     * @param keptNode
     * @param msgHeader
     */
    private void throwConflictException(DependencyNode omittedNode,
	    DependencyNode keptNode, String msgHeader) {
	if (throwExceptionOnConflict) {
	    StringBuilder msg = new StringBuilder();
	    msg.append(String.format("\n" + msgHeader, stringify(keptNode),
		    stringify(omittedNode)));
	    msg.append("\nKept dependency parent tree:\n");
	    msg.append(printNodeParentsTree(keptNode));
	    msg.append("\nOmitted dependency parent tree:\n");
	    msg.append(printNodeParentsTree(omittedNode));
	    msg
		    .append("\nIf You want to ignore the conflict please set \"ignore.dep.conflict\" property to true\n");
	    throw new IllegalStateException(msg.toString());
	}
    }

    /**
     * Helper method which prints (to string) all parents of given node in a
     * form of tree.
     * 
     * @param node
     *            which parents are printed
     * @return string containing printed tree of parents
     */
    private String printNodeParentsTree(DependencyNode node) {
	List deps = new ArrayList();
	DependencyNode parent = node;
	deps.add(stringify(node));
	while ((parent = parent.getParent()) != null) {
	    deps.add(0, stringify(parent));
	}
	StringBuilder msg = null;
	int indentCounter = 2;
	while (!deps.isEmpty()) {
	    if (msg == null) {
		msg = new StringBuilder();
		msg.append("  " + deps.remove(0));
	    } else {
		msg.append("\n");
		for (int i = 0; i < indentCounter; i++) {
		    msg.append(" ");
		}
		msg.append(deps.remove(0));
	    }
	    indentCounter += 2;
	}
	return msg.toString();
    }

    protected void addNode(DependencyNode node) {
	try {
	    if (!wasVisited(node)) {
		boolean shouldResolve = true;
		Artifact artifact = node.getArtifact();
		String nodeStr = stringify(node);
		if (artifactDontResolve != null) {
		    if (artifactDontResolve.equals(nodeStr)) {
			shouldResolve = false;
		    }
		}
		if (shouldResolve) {
		    artifactResolver.resolve(artifact, remoteRepositories,
			    localRepository);
		}

		String mvnUrl = String.format("mvn:%s/%s/%s", artifact
			.getGroupId(), artifact.getArtifactId(), artifact
			.getVersion());

		if (shouldResolve) {
		    File localRepoBaseDir = new File(localRepository
			    .getBasedir());
		    File jarPath = new File(localRepoBaseDir, localRepository
			    .pathOf(node.getArtifact()));
		    JarInputStream jio = new JarInputStream(
			    new FileInputStream(jarPath));
		    Manifest manifest = jio.getManifest();
		    Attributes attribs = manifest.getMainAttributes();
		    Object bundleManifestVersion = attribs
			    .getValue("Bundle-ManifestVersion");
		    if (bundleManifestVersion == null) {
			// it means that the jar is not a bundle - it has to be
			// wrapped before installation in OSGi container
			mvnUrl = "wrap:" + mvnUrl;
		    }
		}
		visited.add(stringify(node));
		mvnUrls.add(mvnUrl);
	    }
	} catch (RuntimeException e) {
	    throw e;
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * If this method returns true then it means that nodes children should be
     * visited. True is returned only if node is in scope and if it was not
     * ommited. It is ensured that node with given groupId, artifactId and
     * version is visited only once.
     * 
     * When visiting node omitted because of conflict, exception is thrown if
     * throwExceptionOnConflict property is true. In case
     * throwExceptionOnConflict is false, the conflict is resolved as suggested
     * by the tree and kept artifact is finally added to mvnUrls list.
     */
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

    /**
     * If this method returns true then it means that the next sibling should be
     * visited. Because all nodes should be visited this methods always returns
     * true.
     * 
     * If node wasn't visited, it is in the scope, and was not omitted then it
     * is added to mvnUrls list.
     */
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

    public List getMvnUrls() {
	return mvnUrls;
    }

    public void setRemoteRepositories(List remoteRepositories) {
	this.remoteRepositories = remoteRepositories;
    }

}
