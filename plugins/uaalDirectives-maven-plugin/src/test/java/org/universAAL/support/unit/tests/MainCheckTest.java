/*******************************************************************************
 * Copyright 2014 Universidad Politécnica de Madrid
 * Copyright 2014 Fraunhofer-Gesellschaft - Institute for Computer Graphics Research
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

package org.universAAL.support.unit.tests;

import java.io.File;

import org.universAAL.support.directives.checks.MainMethodCheck;

import junit.framework.TestCase;

/**
 * @author amedrano
 *
 */
public class MainCheckTest extends TestCase{

    public void testCommentedMain(){
	MainMethodCheck mmc = new MainMethodCheck();
	assertTrue(mmc.passesTest(new File("./src/test/resources/CommentedMain.java")));
    }
    
    public void testMain(){
	MainMethodCheck mmc = new MainMethodCheck();
	assertFalse(mmc.passesTest(new File("./src/test/resources/WithMain.java")));
    }
    
    public void testMain2(){
	MainMethodCheck mmc = new MainMethodCheck();
	assertFalse(mmc.passesTest(new File("./src/test/resources/GUIPacketSniffer.java")));
    }
}
