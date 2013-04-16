package org.universAAL.maven;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.universAAL.maven.treebuilder.MyDependencyNode;

/**
 * This DepepdencyVistor traverses depedency tree in depth-first manner. Visitor
 * needs two collections: nodesByArtifactId and versionsByArtifactId which map
 * respective stringified artifact representations to DependencyNodes. If
 * visitor encounters node which was omitted, then it checks the reason for
 * omitting. If it was omitted because of duplication then visitor looks up the
 * kept node and continues traversing from it. If it was omitted because of
 * conflict then an exception with meaningful message is thrown, unless the
 * visitor is instructed to not throw exception by passing
 * throwExceptionOnConflict parameter to the constructor.
 * 
 * After ending a visit to node which was not ommitted the node's artifact is
 * converted to mvn url and aded to mvnUrls list. If the artifact is not a
 * bundle then mvn url is preceded with "wrap:" protocol. After visit is ended
 * all mvnUrls in required launch order are contained in mvnUrl list.
 * 
 * @author rotgier
 * 
 */
public class LaunchOrderDependencyNodeVisitor extends FilteringVisitorSupport
	implements DependencyNodeVisitor {

    /**
     * Maven local repository.
     */
    private ArtifactRepository localRepository;

    /**
     * Maven artifact resolver.
     */
    private ArtifactResolver artifactResolver;

    /**
     * Stringified representation of artifact which should not be resolved.
     */
    private String artifactDontResolve;

    /**
     * If this is true than it means that the execution list is created on
     * behalf of pom (e.g. parent pom). In such case elements of the list should
     * not be resolver.
     */
    private boolean visitingOnPomBehalf = false;

    /**
     * After finishing the visit this list contains mvnUrls in launch order of
     * all artifacts needed for processed run configuration.
     */
    private final List mvnUrls = new ArrayList();

    /**
     * Mapping of stringified artifacts (groupId:artifactId:version) to nodes in
     * the dependency tree. Nodes contain information about children. Duplicates
     * of nodes are not included in the mapping.
     */
    private Map nodesByArtifactId = new HashMap();

    /**
     * Mapping of stringified artifacts (groupId:artifactId) to its version.
     */
    private Map versionsByArtifactId = new HashMap();

    /**
     * Core uAAL artifacts which should be excluded from created execution list.
     */
    private Set<String> stringifiedExcludedCoreArtifacts;

    /**
     * Whether exception should be thrown in case of conflict in artifact
     * version.
     */
    private boolean throwExceptionOnConflict = true;

    /**
     * Directives configured via <configuration> in pom file, setting the
     * startlevel and/or nostart parameters to specified artifacts
     */
    private StartSpec[] startSpecs;

    /**
     * Constructor of LaunchOrderDependencyNodeVisitor.
     * 
     * @param log
     *            object for logging
     * @param nodesByArtifactId
     *            mapping of stringified artifacts (groupId + artifactId +
     *            version) to nodes in the dependency tree. Nodes contain
     *            information about children. Duplicates of nodes are not
     *            included in the mapping.
     * @param versionsByArtifactId
     *            mapping of stringified artifacts without version (groupId +
     *            artifactId) to stringified artifacts with version.
     * @param throwExceptionOnConflict
     *            flag which turns turning exception on conflict
     * @param localRepository
     *            maven local repository
     * @param artifactResolver
     *            maven artifact resolver
     * @param dontResolve
     *            artifact which should not be resolved
     */
    public LaunchOrderDependencyNodeVisitor(final Log log,
	    final Map nodesByArtifactId, final Map versionsByArtifactId,
	    final boolean throwExceptionOnConflict,
	    final ArtifactRepository localRepository,
	    final ArtifactResolver artifactResolver,
	    final Artifact dontResolve, final StartSpec[] startSpecs) {
	super(log);
	this.localRepository = localRepository;
	this.nodesByArtifactId = nodesByArtifactId;
	this.versionsByArtifactId = versionsByArtifactId;
	this.throwExceptionOnConflict = throwExceptionOnConflict;
	this.artifactResolver = artifactResolver;
	if (dontResolve != null) {
	    this.artifactDontResolve = FilteringVisitorSupport
		    .stringify(dontResolve);
	    if ("pom".equals(dontResolve.getType())) {
		visitingOnPomBehalf = true;
	    }
	}
	this.startSpecs = startSpecs;
    }

    /**
     * If throwExceptionOnConflict is true, this method throws exception about
     * conflict between omitted and kept node.
     * 
     * @param omittedNode
     *            omitted node
     * @param keptNode
     *            kept node
     * @param msgHeader
     *            msg template
     */
    private void throwConflictException(final DependencyNode omittedNode,
	    final DependencyNode keptNode, final String msgHeader) {
	if (throwExceptionOnConflict) {
	    StringBuilder msg = new StringBuilder();
	    msg.append(String.format("\n" + msgHeader, stringify(keptNode),
		    stringify(omittedNode)));
	    msg.append("\nKept dependency parent tree:\n");
	    msg.append(printNodeParentsTree(keptNode));
	    msg.append("\nOmitted dependency parent tree:\n");
	    msg.append(printNodeParentsTree(omittedNode));
	    msg.append("\nIf You want to ignore the conflict please set"
		    + "\"ignore.dep.conflict\" property to true\n");
	    throw new IllegalStateException(msg.toString());
	}
    }

    /**
     * Helper method which prints (to string) all parents of given node in a
     * form of tree.
     * 
     * @param node
     *            which parents are printed
     * @return string containing printed tree of parents
     */
    private String printNodeParentsTree(final DependencyNode node) {
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

    /**
     * Adds node to the execution list.
     * 
     * @param node
     *            which should be added.
     */
    protected final void addNode(final DependencyNode node) {
	try {
	    if (!wasVisited(node)
	    /*
	     * Here all core artifacts which were detected at each RootNode are
	     * excluded.
	     */
	    && (!stringifiedExcludedCoreArtifacts.contains(stringify(node)))) {
		boolean shouldResolve = true;
		Artifact artifact = node.getArtifact();
		String nodeStr = stringify(node);
		if (artifactDontResolve != null) {
		    if (artifactDontResolve.equals(nodeStr)) {
			shouldResolve = false;
		    }
		}
		if (visitingOnPomBehalf) {
		    shouldResolve = false;
		}
		String mvnUrl = String.format("mvn:%s/%s/%s", artifact
			.getGroupId(), artifact.getArtifactId(), artifact
			.getVersion());
		if (shouldResolve) {
		    MyDependencyNode myNode = (MyDependencyNode) node;
		    artifactResolver.resolve(artifact, myNode
			    .getRemoteRepositories(), localRepository);
		    File localRepoBaseDir = new File(localRepository
			    .getBasedir());
		    File jarPath = new File(localRepoBaseDir, localRepository
			    .pathOf(node.getArtifact()));
		    JarInputStream jio = new JarInputStream(
			    new FileInputStream(jarPath));
		    Manifest manifest = jio.getManifest();
		    Object bundleManifestVersion = null;
		    if (manifest != null) {
			Attributes attribs = manifest.getMainAttributes();
			bundleManifestVersion = attribs
				.getValue("Bundle-ManifestVersion");
		    }
		    if (manifest == null || bundleManifestVersion == null) {
			// it means that the jar is not a bundle - it has to be
			// wrapped before installation in OSGi container
			mvnUrl = "wrap:" + mvnUrl;
		    }
		    jio.close();
		}

		// customizing starting of bundles configured in pom file
		if (startSpecs != null) {
		    for (StartSpec s : startSpecs) {
			if (artifact.getGroupId().equals(s.getGroupId())
				&& artifact.getArtifactId().equals(
					s.getArtifactId())) {
			    Integer level = s.getStartlevel();
			    if (level != null) {
				mvnUrl += "@" + level;
			    }
			    if (s.isNostart()) {
				mvnUrl += "@nostart";
			    }
			}
		    }
		}

		getVisited().add(stringify(node));
		mvnUrls.add(mvnUrl);
	    }
	} catch (RuntimeException e) {
	    throw e;
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * If this method returns true then it means that nodes children should be
     * visited. True is returned only if node is in scope and if it was not
     * ommited. It is ensured that node with given groupId, artifactId and
     * version is visited only once.
     * 
     * When visiting node omitted because of conflict, exception is thrown if
     * throwExceptionOnConflict property is true. In case
     * throwExceptionOnConflict is false, the conflict is resolved as suggested
     * by the tree and kept artifact is finally added to mvnUrls list.
     * 
     * @param node
     *            which should be checked for need of visiting
     * @return true if node needs visiting, false otherwise
     */
    public final boolean visit(final DependencyNode node) {
	if (wasVisited(node)) {
	    return false;
	}
	if (isInScope(node)) {
	    switch (node.getState()) {
	    case DependencyNode.OMITTED_FOR_CONFLICT:
	    case DependencyNode.OMITTED_FOR_DUPLICATE:
		Artifact keptArtifact = node.getRelatedArtifact();
		if (keptArtifact == null) {
		    throw new RuntimeException(
			    "keptArtifact of ommited artifact is null: "
				    + node.getArtifact());
		}
		DependencyNode keptNode = (DependencyNode) nodesByArtifactId
			.get(FilteringVisitorSupport.stringify(keptArtifact));
		if (keptNode == null) {
		    /*
		     * keptNode can be null if dependency was conflicted and was
		     * overridden with other conflicting dependency e.g. having
		     * mw.data.representation in versions 0.3.1, 0.3.2,
		     * 1.0.0(resolved version) can impose that 0.3.1 is
		     * conflicted with 0.3.2
		     * 
		     * This can happen in both CONFLICT and DUPLICATE state. So
		     * to overcome this the versionsByArtifactId mapping is
		     * used.
		     */
		    keptNode = (DependencyNode) nodesByArtifactId
			    .get(versionsByArtifactId
				    .get(stringifyNoVersion(keptArtifact)));
		}
		if (keptNode == null) {
		    throw new IllegalStateException("Cannot find keptNode");
		}
		if (node.getState() == DependencyNode.OMITTED_FOR_CONFLICT) {
		    throwConflictException(node, keptNode,
			    "There is a conflict between kept dependency "
				    + "%s and omitted dependency %s");
		}

		keptNode.accept(this);
		return false;
	    default:
		break;
	    }
	    return true;
	}
	return false;
    }

    /**
     * If this method returns true then it means that the next sibling should be
     * visited. Because all nodes should be visited this methods always returns
     * true.
     * 
     * If node wasn't visited, it is in the scope, and was not omitted then it
     * is added to mvnUrls list.
     * 
     * @param node
     *            which visiting should be finished
     * @return this method always returns true
     */
    public final boolean endVisit(final DependencyNode node) {
	if (!wasVisited(node)) {
	    if (isInScope(node)) {
		switch (node.getState()) {
		case DependencyNode.OMITTED_FOR_DUPLICATE:
		case DependencyNode.OMITTED_FOR_CONFLICT:
		    break;
		default:
		    if (!"pom".equals(node.getArtifact().getType())) {
			if (!node.getArtifact().getArtifactId().endsWith(
				"composite")) {
			    addNode(node);
			}
		    }
		}
	    }
	}
	return true;
    }

    /**
     * Gets execution list containing mvn urls.
     * 
     * @return execution list.
     */
    public final List getMvnUrls() {
	return mvnUrls;
    }

    /**
     * Sets core uAAL artifacts which should be excluded from created execution
     * list.
     * 
     * @param excludedCoreArtifacts
     *            list of artifacts to be excluded.
     */
    public final void setExcludedCoreArtifacts(
	    final List<ResolutionNode> excludedCoreArtifacts) {
	stringifiedExcludedCoreArtifacts = new HashSet<String>();
	for (ResolutionNode resolutionNode : excludedCoreArtifacts) {
	    stringifiedExcludedCoreArtifacts.add(stringify(resolutionNode
		    .getArtifact()));
	}
    }

}
