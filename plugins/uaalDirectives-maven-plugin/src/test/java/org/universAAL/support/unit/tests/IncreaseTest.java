package org.universAAL.support.unit.tests;

import junit.framework.TestCase;

import org.universAAL.support.directives.procedures.IncreaseVersionProcedure;

public class IncreaseTest extends TestCase {

	public void test1() {
		assertEquals(IncreaseVersionProcedure.nextDevelopmentVersion("1.0.0"), "1.0.1-SNAPSHOT");
		assertEquals(IncreaseVersionProcedure.nextDevelopmentVersion("1.0.1-SNAPSHOT"), "1.0.2-SNAPSHOT");
		assertEquals(IncreaseVersionProcedure.nextDevelopmentVersion("1.1.115-SNAPSHOT"), "1.1.116-SNAPSHOT");
	}
	
}
