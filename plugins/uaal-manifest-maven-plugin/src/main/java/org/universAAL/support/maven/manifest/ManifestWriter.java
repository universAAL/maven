/*	
	Copyright 2007-2014 Fraunhofer IGD, http://www.igd.fraunhofer.de
	Fraunhofer-Gesellschaft - Institute for Computer Graphics Research
	
	See the NOTICE file distributed with this work for additional 
	information regarding copyright ownership
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	  http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package org.universAAL.support.maven.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.plugin.logging.Log;

public class ManifestWriter {
    private File manifestOutput;
    private Manifest man;
    private Log log;

    public ManifestWriter(Log log, File file) {
	this.log = log;
	manifestOutput = file;
	//manifestOutput = new File(file, "META-INF");
	try {
	    //manifestOutput.mkdirs();
	    //manifestOutput = new File(manifestOutput, "uaal-manifest.mf");
	    manifestOutput.createNewFile();
	} catch (IOException e) {
	}
	man = new Manifest();
    }

    public ManifestWriter(Log log, String filename) {
	this(log, new File(filename));
    }

    public void write(PermissionMap map) {
	// prepare manifest file
	man.getMainAttributes().putValue(
		Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
	for (String key : map.keySet()) {
	    ArrayList<Permission> lst = map.get(key);

	    String val = "";
	    for (Permission p : lst) {
		val += p;
	    }

	    if (val.length() != 0) {
		man.getMainAttributes().putValue("App-permissions-" + key, val);
		// System.out.println("-- adding key: " + key);
	    }
	}

	// write to file
	try {
	    man.write(new FileOutputStream(manifestOutput));
	} catch (FileNotFoundException e) {
	    String filename = "";
	    try {
		filename = manifestOutput.getCanonicalPath();
	    } catch (IOException e1) {
	    }

	    log.info("output file (" + filename
		    + ") could not be created, skipping manifest creation.");
	    // e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
