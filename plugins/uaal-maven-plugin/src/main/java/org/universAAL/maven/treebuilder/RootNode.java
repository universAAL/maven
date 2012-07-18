package org.universAAL.maven.treebuilder;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.shared.dependency.tree.DependencyNode;

/**
 * Helper class for passing arguments and return values to/from methods. It
 * aggregates a rootnode of resolved dependency tree and list of remote
 * repositories used during the resolve proces.
 * 
 * @author rotgier
 * 
 */
public class RootNode {
    public RootNode(final DependencyNode rootNode) {
	this.rootNode = rootNode;
    }

    public DependencyNode rootNode;
    public List remoteRepositories;
    public List<ResolutionNode> excludedCoreArtifacts = new ArrayList<ResolutionNode>();
}
