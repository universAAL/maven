package org.universAAL.maven.treebuilder;

import java.util.List;

import org.apache.maven.project.MavenProject;

public class MavenProjectDescriptor {
    
    MavenProject project;
    List remoteRepositories;
    boolean transitive = true;
    
    public MavenProjectDescriptor(MavenProject project) {
	this.project = project;
    }
    
    public MavenProjectDescriptor(MavenProject project, List remoteRepositories, boolean transitive) {
	this.project = project;
	this.remoteRepositories = remoteRepositories;
	this.transitive = transitive;
    }

}
