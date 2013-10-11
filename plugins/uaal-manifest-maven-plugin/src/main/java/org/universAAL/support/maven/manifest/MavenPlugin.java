package org.universAAL.support.maven.manifest;

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

import java.util.LinkedList;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;

/**
 * @goal uaalManifest
 */
public class MavenPlugin extends AbstractMojo {
    /**
     * @parameter default-value=
     *            "${project.basedir}/src/main/resources/uaal-manifest.xml"
     */
    private String input;

    /**
     * @parameter default-value=
     *            "${project.basedir}/src/main/resources/META-INF/uaal-manifest.mf"
     */
    private String output;

    @Override
    public void execute() throws MojoExecutionException {
	ManifestReader reader = new ManifestReader(input);
	getLog().debug("Reading File '" + input + "'");
	reader.read();

	ManifestWriter writer = new ManifestWriter(output);
	getLog().debug("Writing to File '" + output + "'");
	writer.write(reader.getResult());
    }

    public void setManifestPath(String path) {
	output = path;
    }

    public void setUaalManifestPath(String uaalPath) {
	input = uaalPath;
    }

    
    
    public static void main(String args[]) {
	//Manifest m = new Manifest();
	MavenPlugin p = new MavenPlugin();
	p.setUaalManifestPath("uaal-manifest.xml");
	p.setManifestPath("uaal-manifest.mf");
	try {
	    p.execute();
	} catch (MojoExecutionException e) {
	    e.printStackTrace();
	}
    }
}
