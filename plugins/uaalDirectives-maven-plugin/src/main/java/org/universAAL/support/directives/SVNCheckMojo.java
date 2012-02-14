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
import org.apache.maven.plugin.MojoFailureException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
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

    /**
     * Message content when check fails
     */
    private static final String SCM_NOT_CONFIGURED = "It seems the POM does not contain a SCM tag, or the SCM does not match the actual SVN repository URL.";
    
    /** @parameter default-value="${project}" */
    private org.apache.maven.project.MavenProject mavenProject;


    public void execute() throws MojoFailureException {

	SVNClientManager cli = SVNClientManager.newInstance();
	SVNStatus status;
	this.getLog().debug("checking svn for " + mavenProject.getBasedir().getPath());
	try {
	    status = cli.getStatusClient().doStatus(mavenProject.getBasedir(), false);
	    if (status != null) {
		getLog().debug("not null Status");
		SVNURL url = status.getURL();
		if (url != null){
		    String surl = url.toDecodedString();
		    getLog().debug("found URL	: " + surl);
		    getLog().debug("comparing with	: " + mavenProject.getScm().getConnection());
		    getLog().debug("comparing with	: " + mavenProject.getScm().getDeveloperConnection());
		    if (!mavenProject.getScm().getConnection().endsWith(surl)
			    && !mavenProject.getScm().getDeveloperConnection().endsWith(surl)){
			throw new MojoFailureException(SCM_NOT_CONFIGURED);
		    }
		    else{
			getLog().info("SCM and SVN info are in sync.");
		    }
		}
		else{
		    getLog().error("unable to find URL from svn info.");
		}
	    }
	    else{
		getLog().warn("directory seems not to be a local SVN working copy.");
	    }
	} catch (SVNException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    getLog().warn("SVN Error.");
	}

    }
}
