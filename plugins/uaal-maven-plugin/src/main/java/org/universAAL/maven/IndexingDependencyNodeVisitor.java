package org.universAAL.maven;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private int depthLevel = 0;

    public IndexingDependencyNodeVisitor(Log log,
	    ArtifactFactory artifactFactory,
	    MavenProjectBuilder mavenProjectBuilder, List remoteRepositories,
	    ArtifactRepository localRepository, boolean throwExceptionOnConflict) {
	super(log, artifactFactory, mavenProjectBuilder, remoteRepositories,
		localRepository, throwExceptionOnConflict);
    }

    // Gathering information about runtime dependencies is no longer needed
    // because they are handled by the recurse method itself.
    //	
    // protected List getRuntimeDeps(DependencyNode node) {
    // try {
    // List runtimeDeps = new ArrayList();
    // Artifact nodeArtifact = node.getArtifact();
    // Artifact pomArtifact = artifactFactory.createArtifact(nodeArtifact
    // .getGroupId(), nodeArtifact.getArtifactId(), nodeArtifact
    // .getVersion(), "", "pom");
    // MavenProject pomProject = mavenProjectBuilder.buildFromRepository(
    // pomArtifact, remoteRepositories, localRepository);
    // List profiles = pomProject.getModel().getProfiles();
    // if (profiles != null) {
    // for (Object profileObj : profiles) {
    // Profile profile = (Profile) profileObj;
    // if (UAAL_RUNTIME_PROFILE.equals(profile.getId())) {
    // List deps = profile.getDependencies();
    // if (deps != null) {
    // for (Object depObj : deps) {
    // Dependency dep = (Dependency) depObj;
    // Artifact runtimeArtifact = artifactFactory
    // .createArtifact(dep.getGroupId(), dep
    // .getArtifactId(), dep
    // .getVersion(), dep.getScope(),
    // dep.getType());
    // DependencyNode runtimeDepNode = new DependencyNode(
    // runtimeArtifact);
    // runtimeDeps.add(runtimeDepNode);
    // }
    // }
    // }
    // }
    // }
    // return runtimeDeps;
    // } catch (ProjectBuildingException e) {
    // throw new RuntimeException(e);
    // }
    // }
    //
    // private void handleConflict(DependencyNode node, DependencyNode
    // runtimeDep,
    // String artifactStrVersionLess) {
    // DependencyNode conflictedNode = (DependencyNode) nodesByArtifactId
    // .get(versionsByArtifactId.get(artifactStrVersionLess));
    // int conflictedDepth = conflictedNode.getParent().getDepth();
    // DependencyNode omittedNode = null;
    // DependencyNode keptNode = null;
    // node.addChild(runtimeDep);
    // if (node.getDepth() < conflictedDepth) {
    // omittedNode = conflictedNode;
    // node.addChild(runtimeDep);
    // keptNode = runtimeDep;
    // } else {
    // keptNode = conflictedNode;
    // node.addChild(runtimeDep);
    // omittedNode = runtimeDep;
    // }
    // throwConflictException(
    // omittedNode,
    // keptNode,
    // "There is a conflict between RUNTIME kept dependency %s and RUNTIME omitted dependency %s");
    // /*
    // * If exception was not thrown than conflict can be resolved
    // */
    // omittedNode.getParent().removeChild(omittedNode);
    // }
    //
    // private void indexRuntimeDeps(DependencyNode node) {
    // try {
    // List runtimeDeps = getRuntimeDeps(node);
    // for (Object runtimeDepObj : runtimeDeps) {
    // DependencyNode runtimeDep = (DependencyNode) runtimeDepObj;
    // String artifactStrVersionLess = stringifyNoVersion(runtimeDep);
    // String artifactStr = stringify(runtimeDep);
    // if (!nodesByArtifactId.containsKey(artifactStr)) {
    // if (versionsByArtifactId
    // .containsKey(artifactStrVersionLess)) {
    // /* There is a conflict. Report it or resolve it */
    // handleConflict(node, runtimeDep, artifactStrVersionLess);
    // } else {
    // /* New depedency is added */
    // node.addChild(runtimeDep);
    // nodesByArtifactId.put(artifactStr, runtimeDep);
    // versionsByArtifactId.put(artifactStrVersionLess,
    // artifactStr);
    // }
    // } else {
    // /*
    // * Dependency was already indexed. This is a duplicate which
    // * can be ignored.
    // */
    // }
    // }
    // } catch (Exception ex) {
    // throw new RuntimeException(ex);
    // }
    // }

    /**
     * If this method returns true then it means that nodes children should be
     * visited. True is returned only if node is in scope and if it was not
     * ommited. It is ensured that node with given groupId, artifactId and
     * version is visited only once.
     */
    public boolean visit(DependencyNode node) {
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
    public boolean endVisit(DependencyNode node) {
	if (!wasVisited(node)) {
	    if (isInScope(node)) {
		switch (node.getState()) {
		case DependencyNode.OMITTED_FOR_DUPLICATE:
		case DependencyNode.OMITTED_FOR_CONFLICT:
		    break;
		default:
		    String artifactStrVersionLess = stringifyNoVersion(node);
		    String artifactStr = stringify(node);
		    nodesByArtifactId.put(artifactStr, node);
		    if (versionsByArtifactId
			    .containsKey(artifactStrVersionLess)) {
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
		    versionsByArtifactId.put(artifactStrVersionLess,
			    artifactStr);
		    // indexRuntimeDeps(node);
		}
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
