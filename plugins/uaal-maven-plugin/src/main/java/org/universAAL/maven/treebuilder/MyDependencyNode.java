package org.universAAL.maven.treebuilder;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;

public class MyDependencyNode extends DependencyNode {

    private List remoteRepositories;

    public MyDependencyNode(Artifact artifact, List remoteRepositories) {
	super(artifact);
	this.remoteRepositories = remoteRepositories;
    }

    public List getRemoteRepositories() {
	return remoteRepositories;
    }
}
