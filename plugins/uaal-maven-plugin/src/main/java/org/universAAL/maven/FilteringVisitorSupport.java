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

    protected Log log;

    public FilteringVisitorSupport(Log log) {
	this.log = log;
    }

    /**
     * Set for remembering visited nodes.
     */
    protected final Set visited = new HashSet();

    /**
     * Stringify Artifact to string a in a following way:
     * groupId:artifactId:version.
     * 
     * @param artifact
     * @return
     */
    public static String stringify(Artifact artifact) {
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
     * @param artifact
     * @return
     */
    protected String stringify(DependencyNode node) {
	return FilteringVisitorSupport.stringify(node.getArtifact());
    }

    /**
     * Stringify Artifact to a string in a following way: groupId:artifactId.
     * 
     * @param artifact
     * @return
     */
    protected String stringifyNoVersion(Artifact artifact) {
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
     * @param artifact
     * @return
     */
    protected String stringifyNoVersion(DependencyNode node) {
	return stringifyNoVersion(node.getArtifact());
    }

    /**
     * Check if node was visited.
     * 
     * @param node
     * @return
     */
    protected boolean wasVisited(DependencyNode node) {
	return visited.contains(stringify(node));
    }

    /**
     * Check if DepdencyNode is in the scope. Currently COMPILE and RUNTIME
     * scope are taken into account.
     * 
     * @param node
     * @return
     */
    protected boolean isInScope(DependencyNode node) {
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

}
