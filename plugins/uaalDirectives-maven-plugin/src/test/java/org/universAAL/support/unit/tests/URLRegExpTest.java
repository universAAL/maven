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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * @author amedrano
 *
 */
public class URLRegExpTest extends TestCase {
	/**
	 *
	 */
	private static final String APACHE_URL = "http://www.apache.org/licenses/LICENSE-2.0";
	Pattern urlPattern = Pattern
			.compile("([A-Za-z]+):\\/\\/" + "(\\w+(:\\w+)?@)?" + "[\\w\\.]+" + "(:\\d+)?" + "(" + "\\/"
	// + "(\\w*\\/)*"
					+ "([^\\s]+)?" + "(\\?[\\w=&.]*)?" + "(#\\w+)?" + ")?");

	public void test1() {
		String validURL = APACHE_URL;
		assertTrue(urlPattern.matcher(validURL).find());
	}

	public void test2() {
		String END = " blablablablablab ";
		String validURL = "balblalbalbla blab alb zlbalb lab alb lbla bla bla " + APACHE_URL + END;
		Matcher m = urlPattern.matcher(validURL);
		assertTrue(m.find());
		assertEquals(APACHE_URL, m.group());
		assertEquals(' ', validURL.charAt(m.start() - 1));
		assertEquals(' ', validURL.charAt(m.end()));
		assertEquals(END, validURL.subSequence(m.end(), validURL.length()));
	}

}
