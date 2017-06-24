/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 * Copyright 2013 Fraunhofer-Gesellschaft - Institute for Computer Graphics Research
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
import org.universAAL.support.directives.checks.ItestsCheckFix;

/**
 * This Mojo checks (and fixes, if configured to do so) Dependencies for Itests
 * when there are no IntegrationTest implementations.
 *
 * @author amedrano
 *
 * @goal itest-check
 *
 * @phase process-sources
 */
public class ItestsCheckFixMojo extends AbstractFixableCheckMojo {

	/** {@inheritDoc} */
	public APIFixableCheck getFix() {
		return new ItestsCheckFix();
	}

}
