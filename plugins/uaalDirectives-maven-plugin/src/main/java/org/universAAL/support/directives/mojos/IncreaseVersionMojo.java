/*******************************************************************************
 * Copyright 2011 Universidad Politécnica de Madrid
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
import org.universAAL.support.directives.procedures.IncreaseVersionProcedure;

/**
 * This Mojo updates the current project's version to the next development version.
 * <ul>
 * 		<li>1.2.0 turns into 1.2.1-SNAPSHOT
 * 		<li>1.2.1-SNAPSHOT turns into 1.2.2-SNAPSHOT
 * 		<li> etc...
 * <ul>
 * This doesn't affect submodules's parent version, this mojo should be used in conjunction with 
 * <a href="http://mojo.codehaus.org/versions-maven-plugin/">Versions Maven Plugin</a>
 * (Specially usefull is the versions:update-child-modules, which updates the parent's 
 * version of all modules)
 * @author amedrano
 * 
 * @goal increase-version
 * 
 * @requiresDirectInvocation 
 * 
 * @see ChangeVersionMojo
 */
public class IncreaseVersionMojo extends AbstractProcedureMojo{

	/** {@inheritDoc} */
	@Override
	public APIProcedure getProcedure() {
		return new IncreaseVersionProcedure();
	}
}
