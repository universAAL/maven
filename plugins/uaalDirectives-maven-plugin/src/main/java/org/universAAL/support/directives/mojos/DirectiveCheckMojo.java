/*******************************************************************************
 * Copyright 2013 Universidad Polit�cnica de Madrid
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



/**
 * @author amedrano
 *
 */
public class DirectiveCheckMojo extends AbstractFixableCheckMojo {

	private class FullCheck extends AggregatedCheck {

		/** {@inheritDoc} */
		@Override
		public List<APICheck> getCheckList() {
			List<APICheck> list = new ArrayList<APICheck>();
			//list.add();
			return list;
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public APIFixableCheck getFix() {
		return new FullCheck();
	}

}