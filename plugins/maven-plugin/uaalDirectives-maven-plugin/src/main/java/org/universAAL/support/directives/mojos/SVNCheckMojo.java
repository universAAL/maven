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

import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.api.AbstractFixableCheckMojo;
import org.universAAL.support.directives.checks.SVNCheck;

/**
 * Checks (and fixes, if configured to do so) that the SCM defined (or inherited)
 * by the pom matches the actual SVN url. <BR>
 * It checks the SVN metadata to do so, so this mojo should be executed over a valid SVN working copy.
 * @author amedrano
 * 
 * @goal svn-check
 * 
 * @phase process-sources
 */
public class SVNCheckMojo extends AbstractFixableCheckMojo {

	/** {@inheritDoc} */
	@Override
	public APIFixableCheck getFix() {
		return new SVNCheck();
	}


}
