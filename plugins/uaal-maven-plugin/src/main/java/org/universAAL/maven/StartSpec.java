package org.universAAL.maven;

public class StartSpec {

    /**
     * groupId
     * 
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * artifactId
     * 
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * startlevel
     * 
     * @parameter
     */
    private Integer startlevel;

    /**
     * nostart
     * 
     * @parameter
     */
    private boolean nostart;

    public String getGroupId() {
	return groupId;
    }

    public String getArtifactId() {
	return artifactId;
    }

    public Integer getStartlevel() {
	return startlevel;
    }

    public boolean isNostart() {
	return nostart;
    }

    @Override
    public String toString() {
	return String.format("%s:%s %d %s", groupId, artifactId, startlevel,
		nostart);
    }

}
