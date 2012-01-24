package org.universAAL.maven;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
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
    public static final String UAAL_RUNTIME_PROFILE = "uAAL-Runtime";
    
    /**
     * Whether exception should be thrown in case of conflict in artifact
     * version.
     */
    private boolean throwExceptionOnConflict = true;

    protected Log log;

    protected ArtifactFactory artifactFactory;

    protected MavenProjectBuilder mavenProjectBuilder;

    protected List remoteRepositories;

    protected ArtifactRepository localRepository;

    public FilteringVisitorSupport(Log log, ArtifactFactory artifactFactory,
	    MavenProjectBuilder mavenProjectBuilder, List remoteRepositories,
	    ArtifactRepository localRepository, boolean throwExceptionOnConflict) {
	this.log = log;
	this.artifactFactory = artifactFactory;
	this.mavenProjectBuilder = mavenProjectBuilder;
	this.remoteRepositories = remoteRepositories;
	this.localRepository = localRepository;
	this.throwExceptionOnConflict = throwExceptionOnConflict;
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
    protected String stringify(Artifact artifact) {
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
	return stringify(node.getArtifact());
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

    private String printNodeParentsTree(DependencyNode node) {
	List deps = new ArrayList();
	DependencyNode parent = node;
	deps.add(stringify(node));
	while ((parent = parent.getParent()) != null) {
	    deps.add(0, stringify(parent));
	}
	StringBuilder msg = null;
	int indentCounter = 2;
	while (!deps.isEmpty()) {
	    if (msg == null) {
		msg = new StringBuilder();
		msg.append("  " + deps.remove(0));
	    } else {
		msg.append("\n");
		for (int i = 0; i < indentCounter; i++) {
		    msg.append(" ");
		}
		msg.append(deps.remove(0));
	    }
	    indentCounter += 2;
	}
	return msg.toString();
    }

    protected void throwConflictException(DependencyNode omittedNode,
	    DependencyNode keptNode, String msgHeader) {
	if (throwExceptionOnConflict) {
	    StringBuilder msg = new StringBuilder();
	    msg.append(String.format("\n" + msgHeader, stringify(keptNode),
		    stringify(omittedNode)));
	    msg.append("\nKept dependency parent tree:\n");
	    msg.append(printNodeParentsTree(keptNode));
	    msg.append("\nOmitted dependency parent tree:\n");
	    msg.append(printNodeParentsTree(omittedNode));
	    msg
		    .append("\nIf You want to ignore the conflict please set \"ignore.dep.conflict\" property to true\n");
	    throw new IllegalStateException(msg.toString());
	}
    }

}
