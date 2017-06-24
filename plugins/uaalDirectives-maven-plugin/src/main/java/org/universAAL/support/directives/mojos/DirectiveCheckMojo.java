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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProjectBuilder;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.api.AbstractFixableCheckMojo;
import org.universAAL.support.directives.api.AggregatedCheck;
import org.universAAL.support.directives.checks.DecoupleCheck;
import org.universAAL.support.directives.checks.DependencyManagementCheckFix;
import org.universAAL.support.directives.checks.LicenseFileCheckFix;
import org.universAAL.support.directives.checks.LicenseHeaderCheckFix;
import org.universAAL.support.directives.checks.MainMethodCheck;
import org.universAAL.support.directives.checks.MavenCoordinateCheck;
import org.universAAL.support.directives.checks.ModulesCheckFix;

/**
 * This Mojo executes all checks upon a project. If configured to fix, it will
 * perform all fixes available.
 *
 * @author amedrano
 * @goal check
 *
 * @see DecoupleCheckMojo
 * @see DependencyManagementCheckMojo
 * @see ModulesCheckMojo
 * @see NamingCheckMojo
 * @see LicenseMojo
 */
public class DirectiveCheckMojo extends AbstractFixableCheckMojo {

	/**
	 * The projectBuilder to build children modules.
	 *
	 * @component
	 */
	private MavenProjectBuilder mavenProjectBuilder;

	/**
	 * The localRepository reference, necessary to build projects.
	 *
	 * @parameter default-value="${localRepository}"
	 */
	private ArtifactRepository localRepository;

	private class FullCheck extends AggregatedCheck {

		/** {@inheritDoc} */
		@Override
		public List<APICheck> getCheckList() {
			List<APICheck> list = new ArrayList<APICheck>();
			list.add(new ModulesCheckFix());
			list.add(new DependencyManagementCheckFix(mavenProjectBuilder, localRepository));
			list.add(new MavenCoordinateCheck());
			list.add(new DecoupleCheck());
			list.add(new LicenseFileCheckFix());
			list.add(new LicenseHeaderCheckFix());
			list.add(new MainMethodCheck());
			return list;
		}

	}

	/** {@inheritDoc} */
	@Override
	public APIFixableCheck getFix() {
		return new FullCheck();
	}

}
