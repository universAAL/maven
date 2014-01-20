/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.support.directives.procedures;

import org.universAAL.support.directives.api.APIProcedure;
import org.universAAL.support.directives.util.PomFixer;

/**
 * this procedure is intended to ease Release process by:
 * <ol>
 * <li>changing the uAAL.pom parent version
 * <li>changing the imported root poms' versions in dependencyManagement
 * <li>changing the version of itest in dependencyManagement
 * <li>changing the version of uaal-maven-plugin in dependencyManagement
 * <li>changing the version of uaaldirectives-maven-plugin in
 * dependencyManagement
 * <li>changing the version of uaal-manifest-maven-plugin in
 * dependencyManagement
 * <li>the version of uaaldirectives-maven-plugin in reporting
 * </ol>
 * 
 * All versions are set to be the newVersion Parameter passed in the constructor.
 * 
 * @author amedrano
 * 
 */
public class UpdateRootVersionsProcedure extends
	UpdateParentPomInteractiveProcedure implements APIProcedure, PomFixer {

    /**
     * the version to change uAAL.pom and root imports to.
     */
    private String newVersion;

    /**
     * Constructor.
     * @param newVersion the new Version to use for all artifacts affected.
     */
    public UpdateRootVersionsProcedure(String newVersion) {
	super();
	this.newVersion = newVersion;
    }

    @Override
    protected String ask4NewVersion(String groupID, String artifactID,
	    String currentVersion) {
	return newVersion;
    }

}
