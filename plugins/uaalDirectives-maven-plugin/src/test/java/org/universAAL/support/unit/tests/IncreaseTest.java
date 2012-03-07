package org.universAAL.support.unit.tests;

import org.universAAL.support.directives.IncreaseVersionMojo;

import junit.framework.TestCase;

public class IncreaseTest extends TestCase {

	public void test1() {
		assertEquals(IncreaseVersionMojo.nextDevelopmentVersion("1.0.0"), "1.0.1-SNAPSHOT");
		assertEquals(IncreaseVersionMojo.nextDevelopmentVersion("1.0.1-SNAPSHOT"), "1.0.2-SNAPSHOT");
		assertEquals(IncreaseVersionMojo.nextDevelopmentVersion("1.1.115-SNAPSHOT"), "1.1.116-SNAPSHOT");
	}
	
}
