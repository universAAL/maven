/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.universAAL.maven.treebuilder;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.CyclicDependencyException;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.ResolutionListenerForDepMgmt;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.ManagedVersionMap;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.universAAL.maven.FilteringVisitorSupport;
import org.universAAL.maven.UaalCompositeMojo;

/**
 * Class builds one big dependency tree for given list of artifacts. Each
 * artifact in the list is treated as a rootnode of a separate tree. All
 * duplicates and conflicts are detected throughout the big tree. The tree
 * contains not only regular maven dependencies but also runtime dependencies
 * which are extracted from "uAAL-Runtime" profile of each pom. Artifacts of the
 * following scopes are included in the tree:
 * <ul>
 * <li/>regular maven dependencies: compile and runtime
 * <li/>maven dependencies from "uAAL-Runtime" profile: compile, runtime and
 * provided
 * </ul>
 * 
 * Class code was based on the implementation of class
 * org.apache.maven.shared.dependency.tree.DefaultDependencyTreeBuilder present
 * in maven-dependency-tree-1.1.jar and the implementation of class
 * org.apache.maven.artifact.resolver.DefaultArtifactCollector present in
 * maven-artifact-2.2.1.jar. Both classes are licensed under Apache License,
 * Version 2.0.
 * 
 * @author rotgier
 * 
 * 
 */
public class DependencyTreeBuilder {

    public static final String UAAL_RUNTIME_PROFILE = "uAAL-Runtime";

    public static final String UAAL_TEST_RUNTIME_PROFILE = "uAAL-Test-Runtime";

    public static final String PROP_SEPARATED_GROUP_IDS = "separatedGroupIds";

    private ArtifactFactory artifactFactory;

    private MavenProjectBuilder mavenProjectBuilder;

    private ArtifactRepository localRepository;

    private boolean includeTestRuntimes = false;
    
    private boolean useMwComposite = false;
    
    public String mwVersion = null;

    /**
     * If the root artifact has one of separatedGroupIds then this list will
     * contain its .core dependencies. For now It will be only one .core
     * dependency.
     */
    private List<ResolutionNode> separatedArtifactDepsOfRoot = new ArrayList<ResolutionNode>();

    private String stringifiedRoot = null;

    /**
     * Creates instance of DependencyTreeBuilder with needed parameters.
     * 
     * @param artifactFactory
     *            ArtifactFactory object provided by maven.
     * @param mavenProjectBuilder
     *            MavenProjectBuilder object provided by maven.
     * @param localRepository
     *            The maven's local b provided by maven.
     * @param includeTestRuntimes
     *            Indication whether artifacts for test runtime profile should
     *            be also generated.
     */
    public DependencyTreeBuilder(final ArtifactFactory artifactFactory,
	    final MavenProjectBuilder mavenProjectBuilder,
	    final ArtifactRepository localRepository,
	    final boolean includeTestRuntimes, boolean useMwComposite) {
	this.artifactFactory = artifactFactory;
	this.mavenProjectBuilder = mavenProjectBuilder;
	this.localRepository = localRepository;
	this.includeTestRuntimes = includeTestRuntimes;
	this.useMwComposite = useMwComposite;
    }

    /**
     * FireEvent methods are used for sending events related resolution process
     * to the listeners passed as arguments.
     * 
     * @param event
     *            Integer value of the event.
     * @param listener
     *            Listener which will be notified about the event.
     * @param node
     *            Node related to the event.
     */
    private void fireEvent(final int event,
	    final DependencyTreeResolutionListener listener,
	    final ResolutionNode node) {
	fireEvent(event, listener, node, null);
    }

    /**
     * FireEvent methods are used for sending events related resolution process
     * to the listeners passed as arguments.
     * 
     * @param event
     *            Integer value of the event.
     * @param listener
     *            Listener which will be notified about the event.
     * @param node
     *            Node related to the event.
     * @param replacement
     *            Node which will replace the one passed in the previous
     *            argument.
     */
    private void fireEvent(final int event,
	    final DependencyTreeResolutionListener listener,
	    final ResolutionNode node, final ResolutionNode replacement) {
	fireEvent(event, listener, node, replacement, null);
    }

    /**
     * FireEvent methods are used for sending events related resolution process
     * to the listeners passed as arguments.
     * 
     * @param event
     *            Integer value of the event.
     * @param listener
     *            Listener which will be notified about the event.
     * @param node
     *            Node related to the event.
     * @param replacement
     *            Node which will replace the one passed in the previous
     *            argument.
     * @param newRange
     *            A new range which will be applied in case of RESTRICT_RANGE
     *            event.
     */
    private void fireEvent(final int event,
	    final DependencyTreeResolutionListener listener,
	    final ResolutionNode node, final ResolutionNode replacement,
	    final VersionRange newRange) {
	switch (event) {
	case ResolutionListener.TEST_ARTIFACT:
	    listener.testArtifact(node.getArtifact());
	    break;
	case ResolutionListener.PROCESS_CHILDREN:
	    listener.startProcessChildren(node.getArtifact());
	    break;
	case ResolutionListener.FINISH_PROCESSING_CHILDREN:
	    listener.endProcessChildren(node.getArtifact());
	    break;
	case ResolutionListener.INCLUDE_ARTIFACT:
	    listener.includeArtifact(node);
	    break;
	case ResolutionListener.OMIT_FOR_NEARER:
	    listener.omitForNearer(node, replacement);
	    break;
	case ResolutionListener.OMIT_FOR_CYCLE:
	    listener.omitForCycle(node);
	    break;
	case ResolutionListener.UPDATE_SCOPE:
	    listener.updateScope(node, replacement.getArtifact().getScope());
	    break;
	case ResolutionListener.UPDATE_SCOPE_CURRENT_POM:
	    listener.updateScopeCurrentPom(node, replacement.getArtifact()
		    .getScope());
	    break;
	case ResolutionListener.MANAGE_ARTIFACT_VERSION:
	    if (listener instanceof ResolutionListenerForDepMgmt) {
		ResolutionListenerForDepMgmt asImpl = listener;
		asImpl.manageArtifactVersion(node.getArtifact(),
			replacement.getArtifact());
	    } else {
		listener.manageArtifact(node.getArtifact(),
			replacement.getArtifact());
	    }
	    break;
	case ResolutionListener.MANAGE_ARTIFACT_SCOPE:
	    if (listener instanceof ResolutionListenerForDepMgmt) {
		ResolutionListenerForDepMgmt asImpl = listener;
		asImpl.manageArtifactScope(node.getArtifact(),
			replacement.getArtifact());
	    } else {
		listener.manageArtifact(node.getArtifact(),
			replacement.getArtifact());
	    }
	    break;
	case ResolutionListener.SELECT_VERSION_FROM_RANGE:
	    listener.selectVersionFromRange(node.getArtifact());
	    break;
	case ResolutionListener.RESTRICT_RANGE:
	    if (node.getArtifact().getVersionRange().hasRestrictions()
		    || replacement.getArtifact().getVersionRange()
			    .hasRestrictions()) {
		listener.restrictRange(node.getArtifact(),
			replacement.getArtifact(), newRange);
	    }
	    break;
	default:
	    throw new IllegalStateException("Unknown event: " + event);
	}
    }

    /**
     * Checks if scope update related to conflict or duplication of two given
     * artifacts (farthest - lower in the subtree, nearest - higher in the
     * subtree) is needed. And if so, updates the scope of nearest artifact to
     * the scope of farthest artifact.
     * 
     * @param farthest
     *            artifact lower in the subtree (or the one occuring as second
     *            if both artifacts are on the same level of the subtree)
     * @param nearest
     *            artifact higher in the subtree (or the one occuring as first
     *            if both artifacts are on the same level of the subtree).
     * @param listener
     * @return Returns true if scope was updated.
     */
    private boolean checkScopeUpdate(final ResolutionNode farthest,
	    final ResolutionNode nearest) {
	boolean updateScope = false;
	Artifact farthestArtifact = farthest.getArtifact();
	Artifact nearestArtifact = nearest.getArtifact();

	/* farthest is runtime and nearest has lower priority, change to runtime */
	if (Artifact.SCOPE_RUNTIME.equals(farthestArtifact.getScope())
		&& (Artifact.SCOPE_TEST.equals(nearestArtifact.getScope()) || Artifact.SCOPE_PROVIDED
			.equals(nearestArtifact.getScope()))) {
	    updateScope = true;
	}

	/*
	 * farthest is compile and nearest is not (has lower priority), change
	 * to compile
	 */
	if (Artifact.SCOPE_COMPILE.equals(farthestArtifact.getScope())
		&& !Artifact.SCOPE_COMPILE.equals(nearestArtifact.getScope())) {
	    updateScope = true;
	}

	/*
	 * current POM rules all, if nearest is in current pom, do not update
	 * its scope
	 */
	if (nearest.getDepth() < 2 && updateScope) {
	    updateScope = false;
	}

	if (updateScope) {
	    // previously we cloned the artifact, but it is more effecient to
	    // just update the scope
	    // if problems are later discovered that the original object needs
	    // its original scope value, cloning may
	    // again be appropriate
	    nearestArtifact.setScope(farthestArtifact.getScope());
	}

	return updateScope;
    }

    /**
     * Updates node's scope and version according to the dependency management
     * information.
     * 
     * @param node
     *            Node which scope will be updated.
     * @param managedVersions
     *            Map containing all managed versions.
     */
    private void manageArtifact(final ResolutionNode node,
	    final ManagedVersionMap managedVersions) {
	Artifact artifact = (Artifact) managedVersions.get(node.getKey());

	// Before we update the version of the artifact, we need to know
	// whether we are working on a transitive dependency or not. This
	// allows depMgmt to always override transitive dependencies, while
	// explicit child override depMgmt (viz. depMgmt should only
	// provide defaults to children, but should override transitives).
	// We can do this by calling isChildOfRootNode on the current node.

	if (artifact.getVersion() != null
		&& (node.isChildOfRootNode() ? node.getArtifact().getVersion() == null
			: true)) {
	    if (!"org.apache.felix:org.osgi.compendium:jar".equals(node
		    .getKey())) {
		node.getArtifact().setVersion(artifact.getVersion());
	    }
	}

	if (artifact.getScope() != null
		&& (node.isChildOfRootNode() ? node.getArtifact().getScope() == null
			: true)) {
	    if (!"org.apache.felix:org.osgi.compendium:jar".equals(node
		    .getKey())) {
		node.getArtifact().setScope(artifact.getScope());
	    }
	}
    }

    /**
     * Helper method for extracting parent of passed node with a use of
     * reflection.
     * 
     * @param node
     *            Node from which parent is to be extracted.
     * @return Returns extracted parent.
     */
    private ResolutionNode getParent(final ResolutionNode node) {
	try {
	    Field parentsField = node.getClass().getDeclaredField("parent");
	    parentsField.setAccessible(true);
	    return (ResolutionNode) parentsField.get(node);
	} catch (Exception ex) {
	    throw new RuntimeException(ex);
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
    private String printNodeParentsTree(final ResolutionNode node) {
	List<String> parents = new ArrayList<String>();
	parents.add(0, FilteringVisitorSupport.stringify(node.getArtifact()));
	ResolutionNode parent = node;
	while ((parent = getParent(parent)) != null) {
	    parents.add(0,
		    FilteringVisitorSupport.stringify(parent.getArtifact()));
	}
	StringBuilder msg = null;
	int indentCounter = 2;
	for (String parentStr : parents) {
	    if (msg == null) {
		msg = new StringBuilder();
		msg.append("  " + parentStr);
	    } else {
		msg.append("\n");
		for (int i = 0; i < indentCounter; i++) {
		    msg.append(" ");
		}
		msg.append(parentStr);
	    }
	    indentCounter += 2;
	}
	msg.append("\n");
	return msg.toString();
    }

    /**
     * Method resolves provided node with the use of provided
     * ArtifactMetadataSource and taking into account ManagedVersionMap. Output
     * is passed to listeners, passed as argument, which are notified about all
     * events related to dependencies detected in the tree.
     * 
     * @param parentNode
     *            Parent node
     * @param child
     *            Child node
     * @param filter
     *            Filter for filtering artifacts for the resolving process.
     * @param managedVersions
     *            Map of managed versions.
     * @param listener
     *            Listener to be notified about events related to resolution
     *            process.
     * @param source
     *            ArtifactMetadataSource object passed by maven.
     * @param parentArtifact
     *            Parent artifact
     * @return returns true if the child should be recursively resolved.
     * @throws OverConstrainedVersionException
     *             Occurs when ranges exclude each other and no valid value
     *             remains.
     * @throws ArtifactMetadataRetrievalException
     *             Error while retrieving repository metadata from the
     *             repository
     */
    private boolean resolveChildNode(final ResolutionNode parentNode,
	    final ResolutionNode child, final ArtifactFilter filter,
	    final ManagedVersionMap managedVersions,
	    final DependencyTreeResolutionListener listener,
	    final ArtifactMetadataSource source, final Artifact parentArtifact)
	    throws OverConstrainedVersionException,
	    ArtifactMetadataRetrievalException {
	// We leave in optional ones, but don't pick up its dependencies
	if (!child.isResolved()
		&& (!child.getArtifact().isOptional() || child
			.isChildOfRootNode())) {
	    Artifact artifact = child.getArtifact();
	    artifact.setDependencyTrail(parentNode.getDependencyTrail());

	    List childRemoteRepositories = child.getRemoteRepositories();
	    try {
		Object childKey;
		do {
		    childKey = child.getKey();

		    if (managedVersions.containsKey(childKey)) {
			// If this child node is a managed dependency,
			// ensure
			// we are using the dependency management
			// version
			// of this child if applicable b/c we want to
			// use the
			// managed version's POM, *not* any other
			// version's POM.
			// We retrieve the POM below in the retrieval
			// step.
			manageArtifact(child, managedVersions);

			// Also, we need to ensure that any exclusions
			// it presents are
			// added to the artifact before we retrieve the
			// metadata
			// for the artifact; otherwise we may end up
			// with unwanted
			// dependencies.
			Artifact ma = (Artifact) managedVersions.get(childKey);
			ArtifactFilter managedExclusionFilter = ma
				.getDependencyFilter();
			if (null != managedExclusionFilter) {
			    if (null != artifact.getDependencyFilter()) {
				AndArtifactFilter aaf = new AndArtifactFilter();
				aaf.add(artifact.getDependencyFilter());
				aaf.add(managedExclusionFilter);
				artifact.setDependencyFilter(aaf);
			    } else {
				artifact.setDependencyFilter(managedExclusionFilter);
			    }
			}
		    }

		    if (artifact.getVersion() == null) {
			// set the recommended version
			// TODO: maybe its better to just pass the range
			// through to retrieval and use a
			// transformation?
			ArtifactVersion version;
			if (artifact.isSelectedVersionKnown()) {
			    version = artifact.getSelectedVersion();
			} else {
			    // go find the version
			    List versions = artifact.getAvailableVersions();
			    if (versions == null) {
				versions = source.retrieveAvailableVersions(
					artifact, localRepository,
					childRemoteRepositories);
				artifact.setAvailableVersions(versions);
			    }

			    Collections.sort(versions);

			    VersionRange versionRange = artifact
				    .getVersionRange();

			    version = versionRange.matchVersion(versions);

			    if (version == null) {
				if (versions.isEmpty()) {
				    throw new OverConstrainedVersionException(
					    "No versions are present in the repository for the artifact with a range "
						    + versionRange, artifact,
					    childRemoteRepositories);
				}

				throw new OverConstrainedVersionException(
					"Couldn't find a version in "
						+ versions + " to match range "
						+ versionRange, artifact,
					childRemoteRepositories);
			    }
			}

			// this is dangerous because
			// artifact.getSelectedVersion() can
			// return null. However it is ok here because we
			// first check if the
			// selected version is known. As currently coded
			// we can't get a null here.
			artifact.selectVersion(version.toString());
			fireEvent(ResolutionListener.SELECT_VERSION_FROM_RANGE,
				listener, child);
		    }

		    // rotgier: it is not compatible with maven 3
		    // Artifact relocated = source.retrieveRelocatedArtifact(
		    // artifact, localRepository, childRemoteRepositories);
		    // if (relocated != null && !artifact.equals(relocated)) {
		    // relocated.setDependencyFilter(artifact
		    // .getDependencyFilter());
		    // artifact = relocated;
		    // child.setArtifact(artifact);
		    // }
		} while (!childKey.equals(child.getKey()));

		if (parentArtifact != null
			&& parentArtifact.getDependencyFilter() != null
			&& !parentArtifact.getDependencyFilter().include(
				artifact)) {
		    // MNG-3769: the [probably relocated] artifact is
		    // excluded.
		    // We could process exclusions on relocated artifact
		    // details in the
		    // MavenMetadataSource.createArtifacts(..) step, BUT
		    // that would
		    // require resolving the POM from the repository
		    // very early on in
		    // the build.
		    return true;
		}

		ResolutionGroup rGroup = source.retrieve(artifact,
			localRepository, childRemoteRepositories);

		// TODO might be better to have source.retrieve() throw
		// a specific exception for this situation
		// and catch here rather than have it return null
		if (rGroup == null) {
		    // relocated dependency artifact is declared
		    // excluded, no need to add and recurse further
		    return true;
		}
		child.addDependencies(rGroup.getArtifacts(),
			rGroup.getResolutionRepositories(), filter);

	    } catch (CyclicDependencyException e) {
		// would like to throw this, but we have crappy stuff in
		// the repo

		fireEvent(ResolutionListener.OMIT_FOR_CYCLE, listener,
			new ResolutionNode(e.getArtifact(),
				childRemoteRepositories, child));
	    } catch (ArtifactMetadataRetrievalException e) {
		artifact.setDependencyTrail(parentNode.getDependencyTrail());
		throw e;
	    }
	} else {
	    return true;
	}
	return false;
    }

    /**
     * Helper method for changing artifactId suffix from .core to .osgi.
     * 
     * @param artifactId
     *            ArtifactId which suffix is to be changed.
     * @return Returns artifactId with changed suffix.
     */
    private String changeCoreSuffixToOsgi(final String artifactId) {
	return artifactId.substring(0, artifactId.length() - 5) + ".osgi";
    }

    /**
     * Method checks if passed child artifact has one of groupIds specified in
     * separatedGroupIds Set. If so its artifactId is checked, if the ending
     * suffix is ".core". It this is true then there are two cases:
     * <ul>
     * <li>the parent and the child have the same groupId and they artifactIds
     * are different only in suffix (.core != .osgi). In such case method does
     * not change anything, however it checks if versions of the parent and the
     * child are the same. If not an exception is thrown.
     * <li>if the previous does not apply then the suffix of child's artifactId
     * is changed from ".core" to ".osgi.
     * </ul>
     * 
     * @param parentNode
     *            Parent node.
     * @param childNode
     *            Child node.
     * @param separatedGroupIds
     *            List of groupIds which artifacts are separated to .core and
     *            .osgi branches.
     * @param listener
     *            Listener to be notified about events related to resolution
     *            process.
     */
    private void changeArtifactCoreToOsgi(final ResolutionNode parentNode,
	    final ResolutionNode childNode,
	    final Set<String> separatedGroupIds,
	    final DependencyTreeResolutionListener listener) {
	Artifact parent = parentNode.getArtifact();
	Artifact child = childNode.getArtifact();
	if (separatedGroupIds.contains(child.getGroupId())) {
	    String childArtifactId = child.getArtifactId();
	    if (childArtifactId.endsWith(".core")) {
		if (child.getGroupId().equals(parent.getGroupId())) {
		    if (parent.getArtifactId().endsWith(".osgi")) {
			if (changeCoreSuffixToOsgi(childArtifactId).equals(
				parent.getArtifactId())) {
			    /*
			     * rotgier: In this case artifact cannot be changed
			     * because cyclic dependency will be created and
			     * TreeBuilder will miss the actual dependencies of
			     * the .core artifact.
			     * 
			     * However the version check is performed.
			     */
			    if (child.getVersion() != null) {
				if (!parent.getVersion().equals(
					child.getVersion())) {
				    throw new IllegalStateException(
					    "Versions of parent and child are different");
				}
			    } else {
				throw new IllegalStateException(
					"Child version is not present");
			    }
			    if (this.stringifiedRoot
				    .equals(FilteringVisitorSupport
					    .stringify(parent))) {
				this.separatedArtifactDepsOfRoot.add(childNode);
			    }
			    listener.addExcludedCoreArtifact(childNode);
			    return;
			}
		    }
		}
		/*
		 * rotgier: Otherwise the artifactId suffix has to be changed
		 * from .core to .osgi to follow the actual dependencies of the
		 * .osgi branch.
		 */
		Artifact osgiArtifact = null;
		String osgiArtifactId = changeCoreSuffixToOsgi(childArtifactId);
		if (child.getVersion() != null) {
		    osgiArtifact = artifactFactory.createArtifact(
			    child.getGroupId(), osgiArtifactId,
			    child.getVersion(), child.getScope(),
			    child.getType());
		} else {
		    throw new IllegalArgumentException();
		}
		childNode.setArtifact(osgiArtifact);
	    }
	}
    }

    /**
     * The heart of the tree builder. Recursively resolves provided artifact.
     * Output is passed to listeners, passed as argument, which are notified
     * about all dependencies detected in the tree. Resolving of each child node
     * is delegated to resolveChildNode method.
     * 
     * @param originatingArtifact
     *            Rootnode of recursed subtree.
     * @param node
     *            Current node which is resolved.
     * @param resolvedArtifacts
     *            Map which is used for remembering already resolved artifacts.
     *            Artifacts are indexed by a key which calculation algorithm is
     *            the same as the one present in calculateDepKey method. Thanks
     *            to this map, duplicates and conflicts are detected and
     *            resolved.
     * @param managedVersions
     *            Information about dependency management extracted from the
     *            subtree rootnode - a maven project.
     * @param localRepository
     *            Local maven repository.
     * @param remoteRepositories
     *            Remote repositories provided by maven.
     * @param source
     *            ArtifactMetadataSource provided by maven.
     * @param filter
     *            Filter used for unfiltering artifacts which should not be
     *            included in the dependency tree.
     * @param listener
     *            Listener used for providing the output of the resolve process.
     * @param transitive
     *            If this parameter is false than the children of current node
     *            are not resolved.
     * 
     * @throws CyclicDependencyException
     *             Exception thrown when cyclic dependency detected.
     * @throws ArtifactResolutionException
     *             Exception thrown when a problem with artifact resolution
     *             occurs.
     * @throws OverConstrainedVersionException
     *             Occurs when ranges exclude each other and no valid value
     *             remains.
     * @throws ArtifactMetadataRetrievalException
     *             Error while retrieving repository metadata from the
     *             repository.
     * @throws NoSuchFieldException
     *             Signals that the class doesn't have a field of a specified
     *             name.
     * @throws SecurityException
     *             Thrown by the security manager to indicate a security
     *             violation.
     * @throws IllegalAccessException
     *             When illegal access is performed in the curse of java
     *             reflection operations.
     * @throws IllegalArgumentException
     *             Thrown to indicate that an illegal or inappropriate argument
     *             has been passed.
     */
    private void recurse(final Artifact originatingArtifact,
	    final ResolutionNode node, final Map resolvedArtifacts,
	    final ManagedVersionMap managedVersions,
	    final ArtifactRepository localRepository,
	    final List remoteRepositories, final ArtifactMetadataSource source,
	    final ArtifactFilter filter,
	    final DependencyTreeResolutionListener listener,
	    final boolean transitive, final Set<String> separatedGroupIds)
	    throws CyclicDependencyException, ArtifactResolutionException,
	    OverConstrainedVersionException,
	    ArtifactMetadataRetrievalException, SecurityException,
	    NoSuchFieldException, IllegalArgumentException,
	    IllegalAccessException {
	// check for MW bundle; return if a mw bundle
	boolean out = false;
	//System.out.println(" --oa   " + originatingArtifact.getArtifactId());
	if (out) System.out.println(" --node " + node.getArtifact().getArtifactId() + "    " + node.getArtifact().getGroupId());
	if (useMwComposite && UaalCompositeMojo.MW_GROUP_ID.equals(node.getArtifact().getGroupId())) {
	    if (!node.getArtifact().getArtifactId().contains("karaf.feature")) {
		String thisVersion = node.getArtifact().getVersion();
		if (mwVersion == null) {
		    mwVersion = thisVersion;
		} else {
		    if (!mwVersion.equals(thisVersion))
			throw new IllegalStateException(
				"The dependencies have two different version of middleware bundles: " + mwVersion
					+ " and " + thisVersion);
		}
		if (out) System.out.println(" -- return");
		if (out) System.out.println();
		return;
	    } else {
		if (out) System.out.println(" -- node contains karaf.feature");
	    }
	}
	if (out) System.out.println();
	
	// no MW bundle -> go on with recurse
	try {

	    fireEvent(ResolutionListener.TEST_ARTIFACT, listener, node);
	    Object key = node.getKey();

	    // TODO: Does this check need to happen here? Had to add the same
	    // call
	    // below when we iterate on child nodes -- will that suffice?
	    if (managedVersions.containsKey(key)) {
		manageArtifact(node, managedVersions);
	    }

	    List previousNodes = (List) resolvedArtifacts.get(key);
	    if (previousNodes != null) {
		for (Iterator i = previousNodes.iterator(); i.hasNext();) {
		    ResolutionNode previous = (ResolutionNode) i.next();

		    if (previous.isActive()) {
			// Version mediation
			VersionRange previousRange = previous.getArtifact()
				.getVersionRange();
			VersionRange currentRange = node.getArtifact()
				.getVersionRange();

			if (previousRange != null && currentRange != null) {
			    // TODO: shouldn't need to double up on this work,
			    // only
			    // done for simplicity of handling recommended
			    // version but the restriction is identical
			    VersionRange newRange = previousRange
				    .restrict(currentRange);
			    // TODO: ick. this forces the OCE that should have
			    // come
			    // from the previous call. It is still correct
			    if (newRange.isSelectedVersionKnown(previous
				    .getArtifact())) {
				fireEvent(ResolutionListener.RESTRICT_RANGE,
					listener, node, previous, newRange);
			    }
			    previous.getArtifact().setVersionRange(newRange);
			    node.getArtifact().setVersionRange(
				    currentRange.restrict(previousRange));

			    // Select an appropriate available version from the
			    // (now
			    // restricted) range
			    // Note this version was selected before to get the
			    // appropriate POM
			    // But it was reset by the call to setVersionRange
			    // on
			    // restricting the version
			    ResolutionNode[] resetNodes = { previous, node };
			    for (int j = 0; j < 2; j++) {
				Artifact resetArtifact = resetNodes[j]
					.getArtifact();

				// MNG-2123: if the previous node was not a
				// range,
				// then it wouldn't have any available
				// versions. We just clobbered the selected
				// version
				// above. (why? i have no idea.)
				// So since we are here and this is ranges we
				// must
				// go figure out the version (for a third
				// time...)
				if (resetArtifact.getVersion() == null
					&& resetArtifact.getVersionRange() != null) {

				    // go find the version. This is a total
				    // hack.
				    // See previous comment.
				    List versions = resetArtifact
					    .getAvailableVersions();
				    if (versions == null) {
					try {
					    versions = source
						    .retrieveAvailableVersions(
							    resetArtifact,
							    localRepository,
							    remoteRepositories);
					    resetArtifact
						    .setAvailableVersions(versions);
					} catch (ArtifactMetadataRetrievalException e) {
					    resetArtifact
						    .setDependencyTrail(node
							    .getDependencyTrail());
					    throw e;
					}
				    }
				    // end hack

				    // MNG-2861: match version can return null
				    ArtifactVersion selectedVersion = resetArtifact
					    .getVersionRange()
					    .matchVersion(
						    resetArtifact
							    .getAvailableVersions());
				    if (selectedVersion != null) {
					resetArtifact
						.selectVersion(selectedVersion
							.toString());
				    } else {
					throw new OverConstrainedVersionException(
						" Unable to find a version in "
							+ resetArtifact
								.getAvailableVersions()
							+ " to match the range "
							+ resetArtifact
								.getVersionRange(),
						resetArtifact);
				    }
				    fireEvent(
					    ResolutionListener.SELECT_VERSION_FROM_RANGE,
					    listener, resetNodes[j]);
				}
			    }
			}

			// Conflict Resolution
			// TODO: use as conflict resolver(s), chain

			// TODO: should this be part of mediation?
			// previous one is more dominant
			ResolutionNode nearest;
			ResolutionNode farthest;
			if (previous.getDepth() <= node.getDepth()) {
			    nearest = previous;
			    farthest = node;
			} else {
			    nearest = node;
			    farthest = previous;
			}

			if (checkScopeUpdate(farthest, nearest)) {
			    // if we need to update scope of nearest to use
			    // farthest
			    // scope, use the nearest version, but farthest
			    // scope
			    nearest.disable();
			    farthest.getArtifact().setVersion(
				    nearest.getArtifact().getVersion());
			    fireEvent(ResolutionListener.OMIT_FOR_NEARER,
				    listener, nearest, farthest);
			} else {
			    farthest.disable();
			    fireEvent(ResolutionListener.OMIT_FOR_NEARER,
				    listener, farthest, nearest);
			}
		    }
		}
	    } else {
		previousNodes = new ArrayList();
		resolvedArtifacts.put(key, previousNodes);
	    }
	    previousNodes.add(node);

	    if (node.isActive()) {
		fireEvent(ResolutionListener.INCLUDE_ARTIFACT, listener, node);
	    }

	    // don't pull in the transitive deps of a system-scoped dependency.
	    if (node.isActive()
		    && !Artifact.SCOPE_SYSTEM.equals(node.getArtifact()
			    .getScope())) {
		fireEvent(ResolutionListener.PROCESS_CHILDREN, listener, node);
		if (transitive) {
		    Artifact parentArtifact = node.getArtifact();
		    for (Iterator i = node.getChildrenIterator(); i.hasNext();) {
			ResolutionNode child = (ResolutionNode) i.next();
			if (!filter.include(child.getArtifact())) {
			    continue;
			}
			/*
			 * rotgier: In case of regular dependencies provided
			 * scope is simply ignored (artifact versions specified
			 * there conflict with the ones of runtime deps)
			 */
			if (Artifact.SCOPE_PROVIDED.equals(child.getArtifact()
				.getScope())) {
			    continue;
			}
			changeArtifactCoreToOsgi(node, child,
				separatedGroupIds, listener);
			boolean isContinue = resolveChildNode(node, child,
				filter, managedVersions, listener, source,
				parentArtifact);
			if (isContinue) {
			    continue;
			}
			List<String> extractedSeparatedGroupIds = extractSeparatedGroupIds(
				child.getArtifact(), remoteRepositories);
			Set<String> combinedSeparatedGroupIds = new HashSet<String>(
				separatedGroupIds);
			combinedSeparatedGroupIds
				.addAll(extractedSeparatedGroupIds);
			recurse(originatingArtifact, child, resolvedArtifacts,
				managedVersions, localRepository,
				child.getRemoteRepositories(), source, filter,
				listener, true, combinedSeparatedGroupIds);
		    }
		    List runtimeDeps = getRuntimeDeps(node.getArtifact(),
			    managedVersions, remoteRepositories);
		    Field childrenField = node.getClass().getDeclaredField(
			    "children");
		    childrenField.setAccessible(true);
		    List nodesChildren = (List) childrenField.get(node);
		    /* nodesChildren can be empty when dealing with parent POMs */
		    if (nodesChildren == Collections.EMPTY_LIST) {
			nodesChildren = new ArrayList();
			childrenField.set(node, nodesChildren);
		    }
		    for (Object runtimeDepObj : runtimeDeps) {
			DependencyNode runtimeDep = (DependencyNode) runtimeDepObj;
			Artifact artifact = runtimeDep.getArtifact();
			ResolutionNode childRuntime = new ResolutionNode(
				artifact, node.getRemoteRepositories(), node);
			/*
			 * rotgier: In case of runtime dependencies provided
			 * scope should be allowed
			 */
			if (!filter.include(childRuntime.getArtifact())) {

			    if (!Artifact.SCOPE_PROVIDED.equals(artifact
				    .getScope())) {
				continue;
			    }
			}
			changeArtifactCoreToOsgi(node, childRuntime,
				separatedGroupIds, listener);
			boolean isContinue = resolveChildNode(node,
				childRuntime, filter, managedVersions,
				listener, source, parentArtifact);
			if (isContinue) {
			    continue;
			}
			List<String> extractedSeparatedGroupIds = extractSeparatedGroupIds(
				childRuntime.getArtifact(), remoteRepositories);
			Set<String> combinedSeparatedGroupIds = new HashSet<String>(
				separatedGroupIds);
			combinedSeparatedGroupIds
				.addAll(extractedSeparatedGroupIds);
			recurse(originatingArtifact, childRuntime,
				resolvedArtifacts, managedVersions,
				localRepository,
				childRuntime.getRemoteRepositories(), source,
				filter, listener, true,
				combinedSeparatedGroupIds);
			nodesChildren.add(childRuntime);
		    }
		}
		fireEvent(ResolutionListener.FINISH_PROCESSING_CHILDREN,
			listener, node);
	    }
	} catch (Exception ex) {
	    StringBuilder msg = new StringBuilder();
	    msg.append(String
		    .format("\nUnpredicted exception during dependency tree recursion at node %s",
			    FilteringVisitorSupport.stringify(node
				    .getArtifact())));
	    msg.append("\nNode's parent tree:\n");
	    msg.append(printNodeParentsTree(node));
	    throw new IllegalStateException(msg.toString(), ex);
	}
    }

    /**
     * Get the map of managed versions, removing the originating artifact if it
     * is also in managed versions
     * 
     * @param originatingArtifact
     *            artifact we are processing
     * @param managedVersions
     *            original managed versions
     * 
     * @return Returns the map of managed versions.
     */
    private ManagedVersionMap getManagedVersionsMap(
	    final Artifact originatingArtifact, final Map managedVersions) {
	ManagedVersionMap versionMap;
	if (managedVersions != null
		&& managedVersions instanceof ManagedVersionMap) {
	    versionMap = (ManagedVersionMap) managedVersions;
	} else {
	    versionMap = new ManagedVersionMap(managedVersions);
	}

	/*
	 * remove the originating artifact if it is also in managed versions to
	 * avoid being modified during resolution
	 */
	Artifact managedOriginatingArtifact = (Artifact) versionMap
		.get(originatingArtifact.getDependencyConflictId());
	if (managedOriginatingArtifact != null) {
	    // TODO we probably want to warn the user that he is building an
	    // artifact with
	    // different values than in dependencyManagement
	    if (managedVersions instanceof ManagedVersionMap) {
		/*
		 * avoid modifying the managedVersions parameter creating a new
		 * map
		 */
		versionMap = new ManagedVersionMap(managedVersions);
	    }
	    versionMap.remove(originatingArtifact.getDependencyConflictId());
	}

	return versionMap;
    }

    /**
     * Calculates a stringified representation - a key of given dependency. The
     * algorithm present in org.apache.maven.artifact.resolver.ResolutionNode
     * class (maven-artifact-2.2.1.jar) is used.
     * 
     * @param dep
     *            dependency which key is calculated
     * @return stringified representation of dependency in a form:
     *         groupId:artifactId:type:classifier. Classifier is added only when
     *         it is present.
     */
    private String calculateDepKey(final Dependency dep) {
	StringBuffer sb = new StringBuffer();
	sb.append(dep.getGroupId());
	sb.append(":");
	sb.append(dep.getArtifactId());
	sb.append(":");
	sb.append(dep.getType());
	if (dep.getClassifier() != null) {
	    sb.append(":");
	    sb.append(dep.getClassifier());
	}
	return sb.toString();
    }

    /**
     * Method extracts separatedGroupIds - a list of Maven groupIds which
     * artifacts are treated in a special way during tree recursion. It is
     * assumed that artifacts with these groupIds are separated into two
     * artifacts (two parts): one with "core" suffix; one with "osgi" suffix.
     * Each one is later referred to as the counterpart of the other one.
     * Artifact with "core" suffix not always has to be present. It is assumed
     * that if artifact with "osgi" suffix has a "core" counterpart than it is
     * always specified as its direct dependency. When an artifact with
     * separatedGroupId is encountered it is treated in the recurse method in
     * the following way:
     * <ul>
     * <li>if it is "core" artifact and it is beginning of the method than
     * Exception is thrown ("core" artifact of separatedGroupId should be never
     * passed for recursion).
     * <li>if it is "osgi" artifact and it has its "core" counterpart, then this
     * "core" is simply recursed further but its also remember as artifact which
     * will be removed from final composite just before writing its contents to
     * target\artifact.composite
     * <li>
     * if it is "osgi" artifact and it has a dependency to artifact with
     * separatedGroupId and a "core" suffix, then such depedency is changed to
     * its "osgi" counterpart WITH THE SAME VERSION NUMBER. Therefore there is a
     * strong assumption that a separated artifact has always its "osgi" and
     * "core" parts in the same version!!!!
     * </ul>
     * 
     * When tree is built, all dependencies referring to the "core" artifact are
     * changed into dependencies referring to the "osgi" artifact.
     * 
     * @param project
     *            Maven project from which separatedGroupIds will be extracted.
     * @return List of separatedGroupIds.
     */
    private List<String> extractSeparatedGroupIds(final MavenProject project) {
	List<String> separatedGroupIds = new ArrayList<String>();
	Properties properties = project.getProperties();
	if (properties != null) {
	    String separatedGroupIdsStr = properties
		    .getProperty(PROP_SEPARATED_GROUP_IDS);
	    if (separatedGroupIdsStr != null) {
		for (String groupId : separatedGroupIdsStr.split(",")) {
		    separatedGroupIds.add(groupId.trim());
		}
	    }
	}
	return separatedGroupIds;
    }

    /**
     * Extracts separatedGroupIds.
     * 
     * @param artifact
     *            Artifact from which separatedGroupIds should be extracted.
     * @param remoteRepositories
     *            Remote maven repositories used for resolving of passed
     *            artifact.
     * @return List of separatedGroupIds.
     */
    private List<String> extractSeparatedGroupIds(final Artifact artifact,
	    final List remoteRepositories) {
	try {
	    Artifact pomArtifact = artifactFactory.createArtifact(
		    artifact.getGroupId(), artifact.getArtifactId(),
		    artifact.getVersion(), "", "pom");
	    MavenProject pomProject = mavenProjectBuilder.buildFromRepository(
		    pomArtifact, remoteRepositories, localRepository);
	    return extractSeparatedGroupIds(pomProject);
	} catch (ProjectBuildingException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Gets list of DependencyNode
     * 
     * @param deps
     *            List of raw maven Dependencies extracted from the pom file.
     * @param runtimeDeps
     *            List of DependencyNodes extracted from deps List.
     * @param managedVersions
     *            Map containing managed artifact versions.
     */
    private void extractDepsFromProfile(final List deps,
	    final List runtimeDeps, final ManagedVersionMap managedVersions) {
	if (deps != null) {
	    for (Object depObj : deps) {
		Dependency dep = (Dependency) depObj;
		String depKey = calculateDepKey(dep);

		String depVersion = dep.getVersion();
		String depScope = dep.getScope();

		VersionRange versionRange = null;
		if (managedVersions.containsKey(depKey)) {
		    Artifact managedArtifact = (Artifact) managedVersions
			    .get(depKey);
		    if (managedArtifact.getVersion() != null
			    && (depVersion == null)) {
			depVersion = managedArtifact.getVersion();
		    } else if (managedArtifact.getVersionRange() != null
			    && (depVersion == null)) {
			versionRange = managedArtifact.getVersionRange();
		    }
		    if (managedArtifact.getScope() != null
			    && (depScope == null)) {
			depScope = managedArtifact.getScope();
		    }
		}
		Artifact runtimeArtifact = null;
		if (versionRange == null) {
		    runtimeArtifact = artifactFactory.createArtifact(
			    dep.getGroupId(), dep.getArtifactId(), depVersion,
			    depScope, dep.getType());
		} else {
		    runtimeArtifact = artifactFactory.createDependencyArtifact(
			    dep.getGroupId(), dep.getArtifactId(),
			    versionRange, dep.getType(), dep.getClassifier(),
			    depScope);
		}
		DependencyNode runtimeDepNode = new DependencyNode(
			runtimeArtifact);
		runtimeDeps.add(runtimeDepNode);
	    }
	}
    }

    /**
     * Resolves runtime dependencies of given artifact. It is assumed that
     * runtime dependencies are enclosed in a "uAAL-Runtime" maven profile.
     * Dependencies are resolved taking into account dependency management of
     * maven project which subtree is resolved. Thanks to that there is no need
     * to provide versions for runtime artifacts if they are managed in parent
     * poms.
     * 
     * 
     * @param nodeArtifact
     * @param managedVersions
     * @return
     */
    private List getRuntimeDeps(final Artifact nodeArtifact,
	    final ManagedVersionMap managedVersions,
	    final List remoteRepositories) {
	try {
	    List runtimeDeps = new ArrayList();
	    Artifact pomArtifact = artifactFactory.createArtifact(
		    nodeArtifact.getGroupId(), nodeArtifact.getArtifactId(),
		    nodeArtifact.getVersion(), "", "pom");
	    MavenProject pomProject = mavenProjectBuilder.buildFromRepository(
		    pomArtifact, remoteRepositories, localRepository);
	    List profiles = pomProject.getModel().getProfiles();
	    if (profiles != null) {
		for (Object profileObj : profiles) {
		    Profile profile = (Profile) profileObj;
		    if (UAAL_RUNTIME_PROFILE.equals(profile.getId())) {
			List deps = profile.getDependencies();
			extractDepsFromProfile(deps, runtimeDeps,
				managedVersions);
		    }
		    if (this.includeTestRuntimes) {
			if (this.stringifiedRoot.equals(FilteringVisitorSupport
				.stringify(nodeArtifact))) {
			    if (UAAL_TEST_RUNTIME_PROFILE.equals(profile
				    .getId())) {
				List deps = profile.getDependencies();
				extractDepsFromProfile(deps, runtimeDeps,
					managedVersions);
			    }
			}
		    }
		}
	    }
	    return runtimeDeps;
	} catch (ProjectBuildingException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Method builds dependency tree for a list of maven projects. All artifacts
     * in the tree are crosschecked against duplications and conflicts. In each
     * case of duplication, conflict, the case is resolved by omitting artifact
     * which is lower in the tree and keeping artifact which is higher in the
     * tree. If artifacts are on the same level then the one occuring first in
     * the tree is kept.
     * 
     * @param repository
     *            Maven repository.
     * @param factory
     *            Factory used for creating artifacts.
     * @param metadataSource
     *            ArtifactMetadataSource provided by maven.
     * @param projectDescs
     *            list of maven project descriptors. Each descriptor contains
     *            MavenProject, a list of project's remote repositories and a
     *            boolean indicator if the project needs to be resolved
     *            transitively or not.
     * @return a dependency tree as a list of rootnodes (instances of
     *         DependencyNode class) which contain their own subtrees. Each
     *         rootnode corresponds to one maven project provided as argument.
     *         The order of rootnodes list is the same as order of provided
     *         maven projects list.
     * @throws DependencyTreeBuilderException
     *             Notifies about a problem during building the dependency tree.
     * @throws ArtifactMetadataRetrievalException
     *             Signals problem with metadata retieval.
     * @throws InvalidVersionSpecificationException
     *             Informs about invalid version specifications.
     * @throws NoSuchFieldException
     *             Exception related to reflection.
     * @throws SecurityException
     *             Exception thrown by security manager.
     * @throws IllegalAccessException
     *             Illegal access during usage of java reflection.
     * @throws IllegalArgumentException
     *             Illegal argument was passed.
     */
    public List<RootNode> buildDependencyTree(
	    final ArtifactRepository repository, final ArtifactFactory factory,
	    final ArtifactMetadataSource metadataSource,
	    final MavenProjectDescriptor... projectDescs)
	    throws DependencyTreeBuilderException,
	    ArtifactMetadataRetrievalException,
	    InvalidVersionSpecificationException, SecurityException,
	    NoSuchFieldException, IllegalArgumentException,
	    IllegalAccessException {
	ArtifactFilter filter = new ScopeArtifactFilter();
	DependencyTreeResolutionListener listener = new DependencyTreeResolutionListener(
		filter);
	Map resolvedArtifacts = new LinkedHashMap();
	for (MavenProjectDescriptor projectDesc : projectDescs) {
	    MavenProject project = projectDesc.project;
	    try {
		List<String> separatedGroupId = extractSeparatedGroupIds(project);

		// Here you can simply add to create a new list
		List remoteRepositories = projectDesc.remoteRepositories;

		// If artifact is marked in pom as a bundle then it is changed
		// to jar. Retaining bundle can impose problems when the
		// artifact is duplicated or conflicted with other artifact
		// specified as a dependency, because in the
		// dependency there is only jar type specified.
		Artifact originatingArtifact = project.getArtifact();
		this.stringifiedRoot = FilteringVisitorSupport
			.stringify(originatingArtifact);
		if ("bundle".equals(originatingArtifact.getType())) {
		    Artifact changeArtifact = artifactFactory.createArtifact(
			    originatingArtifact.getGroupId(),
			    originatingArtifact.getArtifactId(),
			    originatingArtifact.getVersion(),
			    originatingArtifact.getScope(), "jar");
		    originatingArtifact = changeArtifact;
		}
		ResolutionNode root = new ResolutionNode(originatingArtifact,
			remoteRepositories);

		// If the project is not supposed to be transitively resolved
		// then its dependencies are not added to the root. Moreover the
		// parameter is passed to the recurse method. Thanks to than
		// when transitive is false, resolving of runtime dependencies
		// is not performed.
		if (projectDesc.transitive) {
		    Set<Artifact> dependencyArtifacts = project
			    .getDependencyArtifacts();

		    if (dependencyArtifacts == null) {
			dependencyArtifacts = new LinkedHashSet();
			List dependencies = project.getDependencies();
			for (Object depObj : dependencies) {
			    Dependency dep = (Dependency) depObj;
			    if (dep.isOptional()) {
				// filtering optional dependencies
				continue;
			    }
			    Artifact dependencyArtifact;
			    VersionRange versionRange = VersionRange
				    .createFromVersionSpec(dep.getVersion());
			    dependencyArtifact = factory
				    .createDependencyArtifact(dep.getGroupId(),
					    dep.getArtifactId(), versionRange,
					    dep.getType(), dep.getClassifier(),
					    dep.getScope());
			    if (dep.getExclusions() != null) {
				if (!dep.getExclusions().isEmpty()) {
				    List<String> patterns = new ArrayList<String>();
				    for (Exclusion exclusion : dep
					    .getExclusions()) {
					patterns.add(exclusion.getGroupId()
						+ ":"
						+ exclusion.getArtifactId());
				    }
				    dependencyArtifact
					    .setDependencyFilter(new ExcludesArtifactFilter(
						    patterns));
				}
			    }
			    dependencyArtifacts.add(dependencyArtifact);
			}
		    } else {
			// filtering optional dependencies
			Set<Artifact> filteredArtifacts = new LinkedHashSet();
			for (Artifact a : dependencyArtifacts) {
			    if (!a.isOptional()) {
				filteredArtifacts.add(a);
			    }
			}
			dependencyArtifacts = filteredArtifacts;
		    }

		    for (Artifact depArtifact : dependencyArtifacts) {
			if (depArtifact.getVersion() != null) {
			    if (!depArtifact.getVersion().equals(
				    depArtifact.getBaseVersion())) {
				if (depArtifact.isSnapshot()) {
				    depArtifact.setVersion(depArtifact
					    .getBaseVersion());
				}
			    }
			}
		    }

		    root.addDependencies(dependencyArtifacts,
			    remoteRepositories, filter);
		}

		// Information about managed versions is extracted from the
		// artifact's pom (but also from parent poms and settings.xml
		// file).
		ManagedVersionMap versionMap = getManagedVersionsMap(
			originatingArtifact, project.getManagedVersionMap());

		recurse(originatingArtifact, root, resolvedArtifacts,
			versionMap, localRepository, remoteRepositories,
			metadataSource, filter, listener,
			projectDesc.transitive, new HashSet<String>(
				separatedGroupId));
	    } catch (ArtifactResolutionException exception) {
		throw new DependencyTreeBuilderException(
			"Cannot build project dependency tree", exception);
	    }
	}
	return listener.getRootNodes();
    }

    /**
     * Getter method to separatedArtifactDepsOfRoot.
     * 
     * @return Returns list of separatedArtifactDepsOfRoot.
     */
    public List<ResolutionNode> getSeparatedArtifactDepsOfRoot() {
	return separatedArtifactDepsOfRoot;
    }

}
