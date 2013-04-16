package org.universAAL.maven.treebuilder;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ResolutionListenerForDepMgmt;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;

/**
 * An artifact resolution listener that constructs a dependency tree. The tree
 * is constructed on basis of events related to resolution process.
 * 
 * Class code was based on implementation of class
 * org.apache.maven.shared.dependency.tree.DependencyTreeResolutionListener
 * present in maven-dependency-tree-1.1.jar which is licensed under Apache
 * License, Version 2.0.
 * 
 * @author rotgier
 * @author Edwin Punzalan
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyTreeResolutionListener.java 576969 2007-09-18
 *          16:11:29Z markh $
 */
public class DependencyTreeResolutionListener implements
	ResolutionListenerForDepMgmt {
    // fields -----------------------------------------------------------------

    /**
     * The parent dependency nodes of the current dependency node.
     */
    private final Stack parentNodes;

    /**
     * A map of dependency nodes by their attached artifact.
     */
    private final Map nodesByArtifact;

    /**
     * The root dependency node of the computed dependency tree.
     */
    private List<RootNode> rootNodes;

    private RootNode currentRootNode;

    /**
     * The dependency node currently being processed by this listener.
     */
    private DependencyNode currentNode;

    /**
     * Map &lt; String replacementId, String premanaged version >
     */
    private Map managedVersions = new HashMap();

    /**
     * Map &lt; String replacementId, String premanaged scope >
     */
    private Map managedScopes = new HashMap();

    // constructors -----------------------------------------------------------

    private ArtifactFilter artifactFilter;

    /**
     * Creates a new dependency tree resolution listener that writes to the
     * specified log.
     * 
     * @param artifactFilter
     *            Object for filtering artifacts.
     */
    public DependencyTreeResolutionListener(final ArtifactFilter artifactFilter) {
	this.artifactFilter = artifactFilter;
	parentNodes = new Stack();
	nodesByArtifact = new IdentityHashMap();
	rootNodes = new ArrayList();
	currentNode = null;
    }

    // ResolutionListener methods ---------------------------------------------

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#testArtifact(org
     * .apache.maven.artifact.Artifact)
     */
    public void testArtifact(final Artifact artifact) {
	log("testArtifact: artifact=" + artifact);
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#startProcessChildren
     * (org.apache.maven.artifact.Artifact)
     */
    public void startProcessChildren(final Artifact artifact) {
	if (!artifactFilter.include(artifact)) {
	    return;
	}
	log("startProcessChildren: artifact=" + artifact);

	if (!currentNode.getArtifact().equals(artifact)) {
	    throw new IllegalStateException("Artifact was expected to be "
		    + currentNode.getArtifact() + " but was " + artifact);
	}

	parentNodes.push(currentNode);
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#endProcessChildren
     * (org.apache.maven.artifact.Artifact)
     */
    public void endProcessChildren(final Artifact artifact) {
	if (!artifactFilter.include(artifact)) {
	    return;
	}
	DependencyNode node = (DependencyNode) parentNodes.pop();

	log("endProcessChildren: artifact=" + artifact);

	if (node == null) {
	    throw new IllegalStateException("Parent dependency node was null");
	}

	if (!node.getArtifact().equals(artifact)) {
	    throw new IllegalStateException(
		    "Parent dependency node artifact was expected to be "
			    + node.getArtifact() + " but was " + artifact);
	}
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#includeArtifact
     * (org.apache.maven.artifact.Artifact)
     */
    public void includeArtifact(final ResolutionNode resolutionNode) {
	if (!artifactFilter.include(resolutionNode.getArtifact())) {
	    return;
	}
	log("includeArtifact: artifact=" + resolutionNode.getArtifact());

	DependencyNode existingNode = getNode(resolutionNode.getArtifact());

	/*
	 * Ignore duplicate includeArtifact calls since omitForNearer can be
	 * called prior to includeArtifact on the same artifact, and we don't
	 * wish to include it twice.
	 */
	if (existingNode == null && isCurrentNodeIncluded()) {
	    DependencyNode node = addNode(resolutionNode);

	    /*
	     * Add the dependency management information cached in any prior
	     * manageArtifact calls, since includeArtifact is always called
	     * after manageArtifact.
	     */
	    flushDependencyManagement(node);
	}
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#omitForNearer(org
     * .apache.maven.artifact.Artifact, org.apache.maven.artifact.Artifact)
     */
    public void omitForNearer(final ResolutionNode omittedNode,
	    final ResolutionNode keptNode) {
	if (!artifactFilter.include(omittedNode.getArtifact())) {
	    return;
	}
	// if (1==1) {
	// return;
	// }
	log("omitForNearer: omitted=" + omittedNode.getArtifact() + " kept="
		+ keptNode.getArtifact());

	if (!omittedNode.getArtifact().getDependencyConflictId()
		.equals(keptNode.getArtifact().getDependencyConflictId())) {
	    throw new IllegalArgumentException(
		    "Omitted artifact dependency conflict id "
			    + omittedNode.getArtifact()
				    .getDependencyConflictId()
			    + " differs from kept artifact dependency conflict id "
			    + keptNode.getArtifact().getDependencyConflictId());
	}

	if (isCurrentNodeIncluded()) {
	    DependencyNode omittedDepNode = getNode(omittedNode.getArtifact());

	    if (omittedDepNode != null) {
		removeNode(omittedNode.getArtifact());
	    } else {
		omittedDepNode = createNode(omittedNode);

		currentNode = omittedDepNode;

		if (omittedDepNode.getDepth() == 0) {
		    rootNodes.add(new RootNode(omittedDepNode));
		}
	    }

	    omittedDepNode.omitForConflict(keptNode.getArtifact());

	    /*
	     * Add the dependency management information cached in any prior
	     * manageArtifact calls, since omitForNearer is always called after
	     * manageArtifact.
	     */
	    flushDependencyManagement(omittedDepNode);

	    DependencyNode keptDepNode = getNode(keptNode.getArtifact());

	    if (keptDepNode == null) {
		addNode(keptNode);
	    }
	}
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#updateScope(org
     * .apache.maven.artifact.Artifact, java.lang.String)
     */
    public void updateScope(final ResolutionNode resolutionNode,
	    final String scope) {
	if (!artifactFilter.include(resolutionNode.getArtifact())) {
	    return;
	}
	log("updateScope: artifact=" + resolutionNode.getArtifact()
		+ ", scope=" + scope);

	DependencyNode node = getNode(resolutionNode.getArtifact());

	if (node == null) {
	    // updateScope events can be received prior to includeArtifact
	    // events
	    node = addNode(resolutionNode);
	}

	node.setOriginalScope(resolutionNode.getArtifact().getScope());
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#manageArtifact(
     * org.apache.maven.artifact.Artifact, org.apache.maven.artifact.Artifact)
     */
    public void manageArtifact(final Artifact artifact,
	    final Artifact replacement) {
	if (!artifactFilter.include(artifact)) {
	    return;
	}
	// TODO: remove when ResolutionListenerForDepMgmt merged into
	// ResolutionListener

	log("manageArtifact: artifact=" + artifact + ", replacement="
		+ replacement);

	if (replacement.getVersion() != null) {
	    manageArtifactVersion(artifact, replacement);
	}

	if (replacement.getScope() != null) {
	    manageArtifactScope(artifact, replacement);
	}
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#omitForCycle(org
     * .apache.maven.artifact.Artifact)
     */
    public void omitForCycle(final ResolutionNode resolutionNode) {
	if (!artifactFilter.include(resolutionNode.getArtifact())) {
	    return;
	}
	log("omitForCycle: artifact=" + resolutionNode.getArtifact());

	if (isCurrentNodeIncluded()) {
	    DependencyNode node = createNode(resolutionNode);

	    node.omitForCycle();
	}
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#updateScopeCurrentPom
     * (org.apache.maven.artifact.Artifact, java.lang.String)
     */
    public void updateScopeCurrentPom(final ResolutionNode resolutionNode,
	    final String scopeIgnored) {
	if (!artifactFilter.include(resolutionNode.getArtifact())) {
	    return;
	}
	log("updateScopeCurrentPom: artifact=" + resolutionNode.getArtifact()
		+ ", scopeIgnored=" + scopeIgnored);

	DependencyNode node = getNode(resolutionNode.getArtifact());

	if (node == null) {
	    // updateScopeCurrentPom events can be received prior to
	    // includeArtifact events
	    node = addNode(resolutionNode);
	    // TODO remove the node that tried to impose its scope and add some
	    // info
	}

	node.setFailedUpdateScope(scopeIgnored);
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#selectVersionFromRange
     * (org.apache.maven.artifact.Artifact)
     */
    public void selectVersionFromRange(final Artifact artifact) {
	log("selectVersionFromRange: artifact=" + artifact);

	// TODO: track version selection from range in node (MNG-3093)
    }

    /*
     * @see
     * org.apache.maven.artifact.resolver.ResolutionListener#restrictRange(org
     * .apache.maven.artifact.Artifact, org.apache.maven.artifact.Artifact,
     * org.apache.maven.artifact.versioning.VersionRange)
     */
    public void restrictRange(final Artifact artifact,
	    final Artifact replacement, final VersionRange versionRange) {
	log("restrictRange: artifact=" + artifact + ", replacement="
		+ replacement + ", versionRange=" + versionRange);

	// TODO: track range restriction in node (MNG-3093)
    }

    // ResolutionListenerForDepMgmt methods -----------------------------------

    /*
     * @seeorg.apache.maven.artifact.resolver.ResolutionListenerForDepMgmt#
     * manageArtifactVersion(org.apache.maven.artifact.Artifact,
     * org.apache.maven.artifact.Artifact)
     */
    public void manageArtifactVersion(final Artifact artifact,
	    final Artifact replacement) {
	if (!artifactFilter.include(artifact)) {
	    return;
	}
	log("manageArtifactVersion: artifact=" + artifact + ", replacement="
		+ replacement);

	/*
	 * DefaultArtifactCollector calls manageArtifact twice: first with the
	 * change; then subsequently with no change. We ignore the second call
	 * when the versions are equal.
	 */
	if (isCurrentNodeIncluded()
		&& !replacement.getVersion().equals(artifact.getVersion())) {
	    /*
	     * Cache management information and apply in includeArtifact, since
	     * DefaultArtifactCollector mutates the artifact and then calls
	     * includeArtifact after manageArtifact.
	     */
	    managedVersions.put(replacement.getId(), artifact.getVersion());
	}
    }

    /*
     * @seeorg.apache.maven.artifact.resolver.ResolutionListenerForDepMgmt#
     * manageArtifactScope(org.apache.maven.artifact.Artifact,
     * org.apache.maven.artifact.Artifact)
     */
    public void manageArtifactScope(final Artifact artifact,
	    final Artifact replacement) {
	if (!artifactFilter.include(artifact)) {
	    return;
	}
	log("manageArtifactScope: artifact=" + artifact + ", replacement="
		+ replacement);

	/*
	 * DefaultArtifactCollector calls manageArtifact twice: first with the
	 * change; then subsequently with no change. We ignore the second call
	 * when the scopes are equal.
	 */
	if (isCurrentNodeIncluded()
		&& !replacement.getScope().equals(artifact.getScope())) {
	    /*
	     * Cache management information and apply in includeArtifact, since
	     * DefaultArtifactCollector mutates the artifact and then calls
	     * includeArtifact after manageArtifact.
	     */
	    managedScopes.put(replacement.getId(), artifact.getScope());
	}
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets a list of all dependency nodes in the computed dependency tree.
     * 
     * @return a list of dependency nodes
     * @deprecated As of 1.1, use a {@link CollectingDependencyNodeVisitor} on
     *             the root dependency node
     */
    @Deprecated
    public Collection getNodes() {
	return Collections.unmodifiableCollection(nodesByArtifact.values());
    }

    /**
     * Gets the root dependency node of the computed dependency tree.
     * 
     * @return the root node
     */
    public List<RootNode> getRootNodes() {
	return rootNodes;
    }

    // private methods --------------------------------------------------------

    /**
     * Writes the specified message to the log at debug level with indentation
     * for the current node's depth.
     * 
     * @param message
     *            the message to write to the log
     */
    private void log(final String message) {
	int depth = parentNodes.size();

	StringBuffer buffer = new StringBuffer();

	for (int i = 0; i < depth; i++) {
	    buffer.append("  ");
	}

	buffer.append(message);
    }

    /**
     * Creates a new dependency node for the specified artifact and appends it
     * to the current parent dependency node.
     * 
     * @param resolutionNode
     *            the attached artifact for the new dependency node
     * @return the new dependency node
     */
    private DependencyNode createNode(final ResolutionNode resolutionNode) {
	DependencyNode node = new MyDependencyNode(
		resolutionNode.getArtifact(),
		resolutionNode.getRemoteRepositories());

	if (!parentNodes.isEmpty()) {
	    DependencyNode parent = (DependencyNode) parentNodes.peek();

	    parent.addChild(node);
	}

	return node;
    }

    /**
     * Creates a new dependency node for the specified artifact, appends it to
     * the current parent dependency node and puts it into the dependency node
     * cache.
     * 
     * @param resolutionNode
     *            the attached artifact for the new dependency node
     * @return the new dependency node
     */
    // package protected for unit test
    DependencyNode addNode(final ResolutionNode resolutionNode) {
	DependencyNode node = createNode(resolutionNode);

	DependencyNode previousNode = (DependencyNode) nodesByArtifact.put(
		node.getArtifact(), node);

	if (previousNode != null) {
	    throw new IllegalStateException(
		    "Duplicate node registered for artifact: "
			    + node.getArtifact());
	}

	int depth = node.getDepth();
	if (depth == 0) {
	    currentRootNode = new RootNode(node);
	    rootNodes.add(currentRootNode);
	}

	currentNode = node;

	return node;
    }

    /**
     * Gets the dependency node for the specified artifact from the dependency
     * node cache.
     * 
     * @param artifact
     *            the artifact to find the dependency node for
     * @return the dependency node, or <code>null</code> if the specified
     *         artifact has no corresponding dependency node
     */
    private DependencyNode getNode(final Artifact artifact) {
	return (DependencyNode) nodesByArtifact.get(artifact);
    }

    /**
     * Removes the dependency node for the specified artifact from the
     * dependency node cache.
     * 
     * @param artifact
     *            the artifact to remove the dependency node for
     */
    private void removeNode(final Artifact artifact) {
	DependencyNode node = (DependencyNode) nodesByArtifact.remove(artifact);

	if (!artifact.equals(node.getArtifact())) {
	    throw new IllegalStateException(
		    "Removed dependency node artifact was expected to be "
			    + artifact + " but was " + node.getArtifact());
	}
    }

    /**
     * Gets whether the all the ancestors of the dependency node currently being
     * processed by this listener have an included state.
     * 
     * @return <code>true</code> if all the ancestors of the current dependency
     *         node have a state of <code>INCLUDED</code>
     */
    private boolean isCurrentNodeIncluded() {
	boolean included = true;

	for (Iterator iterator = parentNodes.iterator(); included
		&& iterator.hasNext();) {
	    DependencyNode node = (DependencyNode) iterator.next();

	    if (node.getState() != DependencyNode.INCLUDED) {
		included = false;
	    }
	}

	return included;
    }

    /**
     * Updates the specified node with any dependency management information
     * cached in prior <code>manageArtifact</code> calls.
     * 
     * @param node
     *            the node to update
     */
    private void flushDependencyManagement(final DependencyNode node) {
	Artifact artifact = node.getArtifact();
	String premanagedVersion = (String) managedVersions.get(artifact
		.getId());
	String premanagedScope = (String) managedScopes.get(artifact.getId());

	if (premanagedVersion != null || premanagedScope != null) {
	    if (premanagedVersion != null) {
		node.setPremanagedVersion(premanagedVersion);
	    }

	    if (premanagedScope != null) {
		node.setPremanagedScope(premanagedScope);
	    }

	    premanagedVersion = null;
	    premanagedScope = null;
	}
    }

    /**
     * Getter to nodesByArtifact.
     * 
     * @return Returns nodesByArtifact.
     */
    public Map getNodesByArtifact() {
	return nodesByArtifact;
    }

    /**
     * Adds node to excluded core artifacts.
     * 
     * @param node
     *            Node which is add to excluded core artifacts.
     */
    public void addExcludedCoreArtifact(final ResolutionNode node) {
	currentRootNode.excludedCoreArtifacts.add(node);
    }

}