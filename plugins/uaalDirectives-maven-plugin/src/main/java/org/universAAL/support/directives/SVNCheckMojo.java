package org.universAAL.support.directives;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNStatus;

/**
 * @author amedrano
 * 
 * @goal svn-check
 * 
 * @phase process-sources
 */
public class SVNCheckMojo
    extends AbstractMojo
{
	
	/** @parameter default-value="${project}" */
	private org.apache.maven.project.MavenProject mavenProject;


    public void execute() throws MojoExecutionException {
    	try {
			SVNStatus status = SVNClientManager.newInstance().getStatusClient().doStatus(mavenProject.getBasedir(), false);
			System.out.println(status.getRemoteURL().toDecodedString());
		} catch (SVNException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
