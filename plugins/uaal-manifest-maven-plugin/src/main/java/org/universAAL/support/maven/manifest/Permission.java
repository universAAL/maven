package org.universAAL.support.maven.manifest;

public class Permission {
    public String title = null;
    public String description = null;
    public String serialization = null;

    public boolean hasNull() {
	return title == null || description == null || serialization == null;
    }

    @Override
    public String toString() {
	return title + "---</title>---" + description
		+ "---</description>---" + serialization
		+ "---</serialization>---";
    }
}
