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
import org.universAAL.support.directives.procedures.ChangeVersionProcedure;

/** 
 * Changes the version of a project to the given new Version.
 * This doesn't affect submodules's parent version, this mojo should be used in conjunction with 
 * <a hrfef="http://mojo.codehaus.org/versions-maven-plugin/">Versions Maven Plugin</a>
 * (Specially usefull is the versions:update-child-modules, which updates the parent's 
 * version of all modules)
 * @author amedrano
 * 
 * @goal change-version
 *
 */
public class ChangeVersionMojo extends AbstractProcedureMojo {

    /**
     * The new version to which to set the POM file.
     * @parameter expression="${newVersion}"
     */
    private String newVersion;
	
	/** {@inheritDoc} */
	@Override
	public APIProcedure getProcedure() {
		if (newVersion == null 
				|| newVersion.isEmpty()){
			while (newVersion == null 
				|| newVersion.isEmpty()){
				System.out.print("\nEnter new version: ");
				newVersion = System.console().readLine();
			}
			return new ChangeVersionProcedure(newVersion);
		}
		else {
			return new ChangeVersionProcedure(newVersion);
		}
	}

}
