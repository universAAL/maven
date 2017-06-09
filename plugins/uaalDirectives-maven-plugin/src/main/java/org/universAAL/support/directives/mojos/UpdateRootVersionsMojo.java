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
package org.universAAL.support.directives.mojos;

import org.universAAL.support.directives.api.APIProcedure;
import org.universAAL.support.directives.api.AbstractProcedureMojo;
import org.universAAL.support.directives.procedures.UpdateParentPomInteractiveProcedure;
import org.universAAL.support.directives.procedures.UpdateRootVersionsProcedure;

/**
 * Changes the versions of the one pom and root dependencyManagement imports to
 * the given new Version. sections changed:
 * <ol>
 * <li>the uAAL.pom parent version
 * <li>the imported root poms' versions in dependencyManagement
 * <li>the version of itest in dependencyManagement
 * <li>the version of uaal-maven-plugin in dependencyManagement
 * <li>the version of uaaldirectives-maven-plugin in dependencyManagement
 * <li>the version of uaaldirectives-maven-plugin in reporting
 * </ol>
 * This mojo is only intended for uAAL release process.
 * 
 * @author amedrano
 * 
 * @goal update-roots
 */
public class UpdateRootVersionsMojo extends AbstractProcedureMojo {

	/**
	 * The new version to set all the version references.
	 * 
	 * @parameter expression="${newVersion}"
	 */
	private String newVersion;

	/** {@inheritDoc} */
	@Override
	public APIProcedure getProcedure() {
		if (newVersion == null || newVersion.isEmpty()) {
			return new UpdateParentPomInteractiveProcedure();
		}
		return new UpdateRootVersionsProcedure(newVersion);
	}
}
