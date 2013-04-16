package org.universAAL.maven.treebuilder;

import java.util.List;

import org.apache.maven.project.MavenProject;

/**
 * Descriptor of maven project related to the dependency resolving process. It
 * aggregates maven project, list of remote repositories used for resolving and
 * indication if dependencies should be resolved transitively or not.
 * 
 * @author rotgier
 * 
 */
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
