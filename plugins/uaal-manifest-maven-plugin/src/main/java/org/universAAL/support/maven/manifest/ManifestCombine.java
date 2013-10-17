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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.jar.Manifest;

import org.apache.maven.plugin.logging.Log;

public class ManifestCombine {
    Log log;
    File src;

    ManifestCombine(Log log, File src) {
	this.log = log;
	this.src = src;
    }

    void combine(File target) {
	Manifest man = new Manifest();
	Manifest manTemp = new Manifest();

	readFile(man, src);
	readFile(manTemp, target);
	man.getMainAttributes().putAll(manTemp.getMainAttributes());
	log.info("  writing manifest to file: " + target.toString());
	writeFile(man, target);
    }

    private void readFile(Manifest man, File file) {
	FileInputStream fis = null;
	try {
	    fis = new FileInputStream(file);
	} catch (FileNotFoundException e) {
	    log.debug(getExceptionString(e));
	    return;
	}

	try {
	    man.read(fis);
	} catch (IOException e) {
	    log.debug(getExceptionString(e));
	    return;
	}
    }

    private void writeFile(Manifest man, File file) {
	// write to file
	try {
	    man.write(new FileOutputStream(file));
	} catch (FileNotFoundException e) {
	    String filename = "";
	    try {
		filename = file.getCanonicalPath();
	    } catch (IOException e1) {
	    }

	    log.info("output file (" + filename
		    + ") could not be created, skipping ..");
	    // e.printStackTrace();
	} catch (IOException e) {
	    log.debug(getExceptionString(e));
	    return;
	}
    }

    private String getExceptionString(Exception ex) {
	StringWriter errors = new StringWriter();
	ex.printStackTrace(new PrintWriter(errors));
	return errors.toString();
    }
}
