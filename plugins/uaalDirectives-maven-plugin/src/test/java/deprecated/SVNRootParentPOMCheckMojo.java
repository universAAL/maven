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
package deprecated;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;


/**
 * @author amedrano
 * 
 * @goal parent-check
 * 
 * @phase process-sources
 */
public class SVNRootParentPOMCheckMojo extends AbstractMojo {

    /**
     * Variable to check
     */
    private static final String PROP = "gforge.project.name";
    
    /**
     * Message content when check fails
     */
    private static final String VARIABLE_NOT_CONFIGURED = System
	    .getProperty("line.separator")
	    + "\n"
	    + "Parent Property Conformance Directive Fail :\n"
	    + "It seems the POM does not contain a <" + PROP + "> property, "
	    + "add such property, which must be equal to gforge's short project name."
	    + System.getProperty("line.separator") + "\n";
    
    private static final Object UAAL_GID = "org.universAAL";

    private static final String UAAL_AID = "uAAL.pom";

    /**
     * @parameter expression="${failOnMissMatch}" default-value="false"
     */
    private boolean failOnMissMatch;

    /**
     * @parameter expression="${directive.fix}" default-value="false"
     */
    private boolean fix;

    /** @parameter default-value="${project}" */
    private org.apache.maven.project.MavenProject mavenProject;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
	if (isParentRootPOM(mavenProject)
		&& !hasProperty(mavenProject,PROP)){
	    if(failOnMissMatch){
		throw new MojoFailureException(VARIABLE_NOT_CONFIGURED);
	    }
	    else{
		getLog().warn(VARIABLE_NOT_CONFIGURED);
	    }
	    if (fix){
		String correctValue;
		try {
		    correctValue = SVNCheckMojo.getSVNURL(mavenProject.getBasedir());
		    URL u = new URL(correctValue);
		    correctValue = u.getPath().split("/")[1];
		    getLog().debug("Determined Correct Value for " + PROP + ": " + correctValue);
		    // Reading
		    MavenXpp3Reader reader = new MavenXpp3Reader();
		    Model model = reader.read(new FileInputStream(new File(mavenProject
			    .getFile().getAbsolutePath())));

		    // Editing
		    Properties p = model.getProperties();
		    
		    p.setProperty(PROP, correctValue);
		    model.setProperties(p);

		    // Writing
		    MavenXpp3Writer writer = new MavenXpp3Writer();

		    writer.write(new OutputStreamWriter(new FileOutputStream(new File(
			    mavenProject.getFile().getAbsolutePath()))), model);
		} catch (Exception e) {
		    getLog().error("Unable to Correct POM",e);
		}
	    }
	}

    }

    public static boolean hasProperty(MavenProject mp, String prop) {
	return mp.getProperties().containsKey(prop);
    }

    public static boolean isParentRootPOM(MavenProject mavenProject2) {
	MavenProject parent = mavenProject2.getParent();
	return (parent.getGroupId().equals(UAAL_GID)
		&& parent.getArtifactId().endsWith(UAAL_AID));
    }

}
