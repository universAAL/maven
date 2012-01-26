package org.universAAL.maven.treebuilder;

import org.apache.maven.project.MavenProject;

public class MavenProjectDescriptor {
    
    MavenProject project;
    boolean transitive = true;
    
    public MavenProjectDescriptor(MavenProject project) {
	this.project = project;
    }
    
    public MavenProjectDescriptor(MavenProject project, boolean transitive) {
	this.project = project;
	this.transitive = transitive;
    }

}
