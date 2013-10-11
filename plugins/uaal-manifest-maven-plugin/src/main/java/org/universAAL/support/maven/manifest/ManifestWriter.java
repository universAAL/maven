package org.universAAL.support.maven.manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestWriter {
    private File manifestOutput;
    private Manifest man;

    public ManifestWriter(String filename) {
	manifestOutput = new File(filename);
	man = new Manifest();
    }

    public void write(HashMap<String, ArrayList<Permission>> map) {
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
		man.getMainAttributes().putValue(key, val);
		//System.out.println("-- adding key: " + key);
	    }
	}

	// write to file
	try {
	    man.write(new FileOutputStream(manifestOutput));
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
