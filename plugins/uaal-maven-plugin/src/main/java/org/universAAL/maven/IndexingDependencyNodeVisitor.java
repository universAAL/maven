package org.universAAL.maven;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * This DepepdencyVistor traverses depedency tree in depth-first manner. All
 * nodes which were not ommited are remembered in two maps: nodesByArtifactId
 * and versionsByArtifactId. Maps are available after search is finished.
 * 
 * @author rotgier
 * 
 */
public class IndexingDependencyNodeVisitor extends FilteringVisitorSupport
	implements DependencyNodeVisitor {

    /**
     * Mapping of stringified artifacts (groupId + artifactId + version) to
     * nodes in the dependency tree. Nodes contain information about children.
     * Duplicates of nodes are not included in the mapping.
     */
    private Map nodesByArtifactId = new HashMap();

    /**
     * Mapping of stringified artifacts without version (groupId + artifactId)
     * to stringified artifacts with version.
     * 
     * The thing is that visited depedency tree should have resolved before all
     * duplicates and conflicts. Therefore visitor can assume that artifact with
     * given groupId and versionId should have only one, chosen version. Other
     * versions can be spotted only in duplicated and conflicted nodes.
     */
    private Map versionsByArtifactId = new HashMap();

    public IndexingDependencyNodeVisitor(final Log log) {
	super(log);
    }

    /**
     * If this method returns true then it means that nodes children should be
     * visited. True is returned only if node is in scope and if it was not
     * ommited. It is ensured that node with given groupId, artifactId and
     * version is visited only once.
     */
    public boolean visit(final DependencyNode node) {
	if (wasVisited(node)) {
	    return false;
	}
	if (isInScope(node)) {
	    switch (node.getState()) {
	    case DependencyNode.OMITTED_FOR_DUPLICATE:
	    case DependencyNode.OMITTED_FOR_CONFLICT:
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
     */
    public boolean endVisit(final DependencyNode node) {
	if (!wasVisited(node) && isInScope(node)) {
	    switch (node.getState()) {
	    case DependencyNode.OMITTED_FOR_DUPLICATE:
	    case DependencyNode.OMITTED_FOR_CONFLICT:
		break;
	    default:
		String artifactStrVersionLess = stringifyNoVersion(node);
		String artifactStr = stringify(node);
		nodesByArtifactId.put(artifactStr, node);
		if (versionsByArtifactId.containsKey(artifactStrVersionLess)) {
		    throw new IllegalStateException(
			    String
				    .format(
					    "versionsByArtifactId already contains artifact %s"
						    + "with the following version %s and now version %s is supposed to be added",
					    artifactStrVersionLess,
					    versionsByArtifactId
						    .get(versionsByArtifactId),
					    artifactStr));
		}
		versionsByArtifactId.put(artifactStrVersionLess, artifactStr);
		// indexRuntimeDeps(node);
	    }
	}
	return true;
    }

    public Map getNodesByArtifactId() {
	return nodesByArtifactId;
    }

    public Map getVersionByArtifactId() {
	return versionsByArtifactId;
    }

}
