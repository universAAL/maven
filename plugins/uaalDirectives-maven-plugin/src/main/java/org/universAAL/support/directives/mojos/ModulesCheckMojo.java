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
import org.universAAL.support.directives.checks.ModulesCheckFix;

/**
 * Check (and fixes, if configured to do so) that all sibling folders are listed
 * as modules in the modules section. <BR>
 * Only for parent POM projects.
 *
 * @author amedrano
 *
 * @aggregator
 *
 * @goal modules-check
 *
 * @phase process-sources
 */
public class ModulesCheckMojo extends AbstractFixableCheckMojo {

	/** {@inheritDoc} */
	@Override
	public APIFixableCheck getFix() {
		return new ModulesCheckFix();
	}

}
