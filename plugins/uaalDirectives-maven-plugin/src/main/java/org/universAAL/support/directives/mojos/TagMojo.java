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
import org.universAAL.support.directives.procedures.Tag;

/**
 * Tags the project in an appropiate tag URL, in concordance to T2.3 Directives.
 * @author amedrano
 * 
 * @goal tag
 * 
 * @requiresDirectInvocation 
 */
public class TagMojo extends AbstractProcedureMojo{
    
    /**
     * @parameter expression="${tagWorkingCopy}" default-value="false"
     */
    private boolean tagRemoteHead;


	/** {@inheritDoc} */
	@Override
	public APIProcedure getProcedure() {
		return new Tag(tagRemoteHead);
	}
}
