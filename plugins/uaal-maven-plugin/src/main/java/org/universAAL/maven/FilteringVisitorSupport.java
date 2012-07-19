package org.universAAL.maven;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.tree.DependencyNode;

/**
 * This abstract class is a support for implementations of DependencyNodeVisitor
 * which provides methods for: filtering, stringifying, remembering visited
 * nodes. Class should be extended by implementation of DependencyNodeVisitor.
 * 
 * @author rotgier
 * 
 */
public abstract class FilteringVisitorSupport {

    /**
     * Object for logging.
     */
    private Log log;

    /**
     * Constructor of FilteringVisitorSupport.
     * 
     * @param log
     *            object used for logging.
     */
    public FilteringVisitorSupport(final Log log) {
	this.log = log;
    }

    /**
     * Set for remembering visited nodes.
     */
    private final Set<String> visited = new HashSet<String>();

    /**
     * Stringify Artifact to string a in a following way:
     * groupId:artifactId:version.
     * 
     * @param artifact
     *            which should be stringified
     * @return stringified artifact representation
     */
    public static String stringify(final Artifact artifact) {
	if (artifact.getVersion() == null) {
	    throw new RuntimeException(
		    "Artifact version and version range is null: " + artifact);
	}
	String uniqArtifactId = artifact.getGroupId() + ":"
		+ artifact.getArtifactId() + ":" + artifact.getVersion();
	return uniqArtifactId;
    }

    /**
     * Stringify DependencyNode to a string in a following way:
     * groupId:artifactId:version.
     * 
     * @param node
     *            which should be stringified
     * @return stringified node representation
     */
    protected final String stringify(final DependencyNode node) {
	return FilteringVisitorSupport.stringify(node.getArtifact());
    }

    /**
     * Stringify Artifact to a string in a following way: groupId:artifactId.
     * 
     * @param artifact
     *            which should be stringified
     * @return stringified artifact representation
     */
    protected final String stringifyNoVersion(final Artifact artifact) {
	if (artifact.getVersion() == null) {
	    throw new RuntimeException(
		    "Artifact version and version range is null: " + artifact);
	}
	String uniqArtifactIdVersionLess = artifact.getGroupId() + ":"
		+ artifact.getArtifactId();
	return uniqArtifactIdVersionLess;
    }

    /**
     * Stringify DependencyNode to a string in a following way:
     * groupId:artifactId.
     * 
     * @param node
     *            which should be stringified
     * @return stringified node representation
     */
    protected final String stringifyNoVersion(final DependencyNode node) {
	return stringifyNoVersion(node.getArtifact());
    }

    /**
     * Check if node was visited.
     * 
     * @param node
     *            which should be checked for being visited
     * @return true is node was visited
     */
    protected final boolean wasVisited(final DependencyNode node) {
	return visited.contains(stringify(node));
    }

    /**
     * Check if DepdencyNode is in the scope. Currently COMPILE and RUNTIME
     * scope are taken into account.
     * 
     * @param node
     *            which should be checked for being in scope
     * @return true if node is in scope
     */
    protected final boolean isInScope(final DependencyNode node) {
	String scope = node.getArtifact().getScope();
	if (scope == null) {
	    log.debug("Null Scope For artifact: "
		    + node.getArtifact().getArtifactId());
	    return true;
	}
	if (Artifact.SCOPE_COMPILE.equals(scope)) {
	    return true;
	} else if (Artifact.SCOPE_RUNTIME.equals(scope)) {
	    return true;
	} else if (Artifact.SCOPE_PROVIDED.equals(scope)) {
	    return true;
	}
	return false;
    }

    /**
     * Gets the log object.
     * 
     * @return the log object.
     */
    protected final Log getLog() {
	return log;
    }

    /**
     * Gets set of visited nodes. Each node is in stringified representation.
     * 
     * @return set of visited nodess
     */
    protected final Set<String> getVisited() {
	return visited;
    }

}
