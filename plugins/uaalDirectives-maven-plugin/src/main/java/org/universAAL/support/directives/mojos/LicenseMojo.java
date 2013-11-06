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

import java.util.ArrayList;
import java.util.List;

import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.api.AbstractFixableCheckMojo;
import org.universAAL.support.directives.api.AggregatedCheck;
import org.universAAL.support.directives.checks.LicenseFileCheckFix;
import org.universAAL.support.directives.checks.LicenseHeaderCheckFix;

/**
 * This Mojo checks (and fixes, if configured to do so) for license files.
 * those that should exists (like ASL2.0.txt, MITX.txt, and NOTICE.txt),
 * as well as those that should not exist (like LICENSE.txt).
 * 
 * @author amedrano
 * @goal license-check
 *
 */
public class LicenseMojo extends AbstractFixableCheckMojo {

	/** {@inheritDoc} */
	@Override
	public APIFixableCheck getFix() {
		return new AggregatedCheck() {
			
			@Override
			public List<APICheck> getCheckList() {
				ArrayList<APICheck> list = new ArrayList<APICheck>();
				list.add(new LicenseFileCheckFix());
				list.add(new LicenseHeaderCheckFix());
				return list;
			}
		};
	}

}
