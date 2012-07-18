package org.universAAL.maven.treebuilder;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 * Filter to only retain objects in the following scopes: COMPILE, RUNTIME,
 * PROVIDED.
 * 
 */
public class ScopeArtifactFilter implements ArtifactFilter {
    public boolean include(final Artifact artifact) {
	if (Artifact.SCOPE_COMPILE.equals(artifact.getScope())) {
	    return true;
	} else if (Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
	    return true;
	} else if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
	    return false;
	} else if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
	    return true;
	} else if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
	    return false;
	} else {
	    return true;
	}
    }
}
