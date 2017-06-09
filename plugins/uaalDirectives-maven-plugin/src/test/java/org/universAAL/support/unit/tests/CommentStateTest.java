/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
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

import java.util.regex.Pattern;

import org.universAAL.support.directives.checks.LicenseHeaderCheckFix;

import junit.framework.TestCase;

/**
 * @author amedrano
 *
 */
public class CommentStateTest extends TestCase {

	private void feedString(LicenseHeaderCheckFix.CommentParserState cps, String s) {
		int i = 0;
		while (i < s.length() && cps.next(s.charAt(i))) {
			i++;
		}

	}

	private void processComment(String s) {
		String comment = Pattern.compile("(^//*|/$|\\n\\s*//|\\*)").matcher(s).replaceAll("");
		comment = Pattern.compile("\\s\\s*").matcher(comment).replaceAll(" ").replace("\n", "");
		// System.out.println("[" + comment + "]");
	}

	private static String STUFF = "\tpackage org.universAAL.support.unit.tests;\n\n"
			+ "import org.universAAL.support.directives.checks.LicenseHeaderCheckFix;\nn"
			+ "import junit.framework.TestCase;\t";

	public void test1() {
		String s = "/**************************************\n" + " *          dsfdfsdfsd fsdf sfsd f     \n"
				+ " *  asddasd dsakljierlplias zaluhg,mhzy\n" + " * ñâüöàèùìỳíéá lololololo             \n"
				+ " */";
		LicenseHeaderCheckFix.CommentParserState cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s + STUFF);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s + STUFF);
		assertEquals(s, cps.getString());
		processComment(cps.getString());
	}

	public void test2() {
		String s = "/*                                     \n" + " *          dsfdfsdfsd fsdf sfsd f     \n"
				+ " *  asddasd dsakljierlplias zaluhg,mhzy\n" + " * ñâüöàèùìỳíéá lololololo             \n"
				+ " */";
		LicenseHeaderCheckFix.CommentParserState cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s + STUFF);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s + STUFF);
		assertEquals(s, cps.getString());
		processComment(cps.getString());
	}

	public void test3() {
		String s = "/*\n" + "           dsfdfsdfsd fsdf sfsd f     \n" + "   asddasd dsakljierlplias zaluhg,mhzy\n"
				+ "  ñâüöàèùìỳíéá lololololo             \n" + " */";
		LicenseHeaderCheckFix.CommentParserState cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s + STUFF);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s);
		assertEquals(s, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s + STUFF);
		assertEquals(s, cps.getString());
		processComment(cps.getString());
	}

	public void test4() {
		String s = "//          dsfdfsdfsd fsdf sfsd f     \n" + "\t//  asddasd dsakljierlplias zaluhg,mhzy\n"
				+ " // ñâüöàèùìỳíéá lololololo             \n";
		String s2 = "//          dsfdfsdfsd fsdf sfsd f     \n" + "//  asddasd dsakljierlplias zaluhg,mhzy\n"
				+ "// ñâüöàèùìỳíéá lololololo             \n";
		LicenseHeaderCheckFix.CommentParserState cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s);
		// assertEquals(s, cps.getString())
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s + STUFF);
		assertEquals(s2, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s);
		assertEquals(s2, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s + STUFF);
		assertEquals(s2, cps.getString());
		processComment(cps.getString());
	}

	public void test5() {
		String s = "//          dsfdfsdfsd fsdf sfsd f     \n" + " //  asddasd dsakljierlplias zaluhg,mhzy\n" + "\n"
				+ "\n" + " // ñâüöàèùìỳíéá lololololo             \n";
		String s2 = "//          dsfdfsdfsd fsdf sfsd f     \n" + "//  asddasd dsakljierlplias zaluhg,mhzy\n" + "\n"
				+ "\n" + "// ñâüöàèùìỳíéá lololololo             \n";
		LicenseHeaderCheckFix.CommentParserState cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s);
		assertEquals(s2, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, s + STUFF);
		assertEquals(s2, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s);
		assertEquals(s2, cps.getString());
		cps = new LicenseHeaderCheckFix.CommentParserState();
		feedString(cps, STUFF + s + STUFF);
		assertEquals(s2, cps.getString());
		processComment(cps.getString());
	}
}
