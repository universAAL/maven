/*
Copyright 2011-2014 AGH-UST, http://www.agh.edu.pl
Faculty of Computer Science, Electronics and Telecommunications
Department of Computer Science 

See the NOTICE file distributed with this work for additional
information regarding copyright ownership

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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
