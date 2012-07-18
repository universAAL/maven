package org.universAAL.maven.treebuilder;

import java.util.List;

import org.apache.maven.project.MavenProject;

public class MavenProjectDescriptor {

    MavenProject project;
    List remoteRepositories;
    boolean transitive = true;

    public MavenProjectDescriptor(final MavenProject project) {
	this.project = project;
    }

    public MavenProjectDescriptor(final MavenProject project,
	    final List remoteRepositories, final boolean transitive) {
	this.project = project;
	this.remoteRepositories = remoteRepositories;
	this.transitive = transitive;
    }

}
