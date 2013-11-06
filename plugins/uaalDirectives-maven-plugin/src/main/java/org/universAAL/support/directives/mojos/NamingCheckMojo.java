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

import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.api.AbstractCheckMojo;
import org.universAAL.support.directives.checks.MavenCoordinateCheck;

/**
 * Checks that the groupId, artifactID, Version, and Name are compliant with the naming conventions.
 * <Br>
 * These conventions are specified by defining respectively the following properties:
 * <ul>
 * 		<li>artifactIdMatchString
 * 		<li>groupIdMatchString
 * 		<li>versionMatchString
 * 		<li>nameMatchString
 * </ul>
 * These properties are defined in <a href="http://docs.oracle.com/javase/tutorial/essential/regex/">
 * standard java regexp</a> language; by default they take the value ".*" (anything) if not defined. 
 * These properties shoud be defined in parent POMs.
 * @author amedrano
 *
 * @goal name-check
 */
public class NamingCheckMojo extends AbstractCheckMojo {
	
	/** {@inheritDoc} */
	@Override
	public APICheck getCheck() {
		return new MavenCoordinateCheck();
	}
	


}
