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
import org.universAAL.support.directives.checks.DecoupleCheck;

/**
 * This Mojo will test whether the uAAL projects are being properly decoupled
 * from OSGi. <BR>
 * For a project to be properly decoupled, any OSGi reference should be in a package
 * that explicitly contains "osgi" in its name.
 * 
 * @author amedrano
 * 
 * @goal decouple-check
 */
public class DecoupleCheckMojo extends AbstractCheckMojo {

	/** {@inheritDoc} */
	@Override
	public APICheck getCheck() {
		return new DecoupleCheck();
	}

}
