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
package org.universAAL.support.directives.checks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APIFixableCheck;
import org.universAAL.support.directives.util.SourceChecker;
import org.universAAL.support.directives.util.SourceExplorer;

/**
 * Check all the code has a valid ASL header.
 * 
 * @author amedrano
 *
 */
public class LicenseHeaderCheckFix implements APIFixableCheck, SourceChecker {

	private static final Pattern PATTERN_SPACE = Pattern.compile("\\s\\s*");
	private static final Pattern PATTERN_DELETE = Pattern.compile("(^//*|/$|\\n\\s*//|\\*|;)");
	private static final Pattern PATTERN_LICENSED_UNDER =
			Pattern.compile("[Ll]icensed under .* Apache License.*[Vv]ersion 2\\.0");
	
	private static final String APACHE_ENDING = 
			" Unless required by applicable law or agreed to in writing, software"
			+" distributed under the License is distributed on an \"AS IS\" BASIS,"
			+" WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."
			+" See the License for the specific language governing permissions and"
			+" limitations under the License.";
	private static final String APACHE_LICENSE_HEADER = " under the Apache License, Version 2.0 (the \"License\")"
			+" you may not use this file except in compliance with the License."
			+" You may obtain a copy of the License at"
			+" http://www.apache.org/licenses/LICENSE-2.0"
			+ APACHE_ENDING;

	/** {@inheritDoc} */
	public boolean check(MavenProject mavenproject, Log log)
			throws MojoExecutionException, MojoFailureException {
		SourceExplorer se = new SourceExplorer(this);
		ArrayList<File> conflicted = se.walk(
				mavenproject.getBasedir().getAbsolutePath() + "/src/main/java/");
		if (conflicted.size() > 0) {
			String m = "The following Files seem not to have a propper License Header:\n";
			for (File f : conflicted) {
				m += "\t" + f.getAbsolutePath() + "\n";
			}
			m += "Make sure all your classes have an Apache Software Licence Header\n"
					+ " see license at http://www.apache.org/licenses/LICENSE-2.0";
			throw new MojoFailureException(m);
		}
		return true;
	}

	/** {@inheritDoc} */
	public void fix(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {
		// TODO Auto-generated method stub

	}		

	public boolean passesTest(File sourceFile) {
		try {
			CommentParserState cps = new CommentParserState();
			FileInputStream is = new FileInputStream(sourceFile);
			int c = is.read();
			while (c != 65535
					&& c!= -1
					&& cps.next((char)c)){
				c = (char) is.read();
			}
			is.close();
			if (c != -1){
				// the first comment is read
				String comment = PATTERN_DELETE
						.matcher(cps.getString()).replaceAll("");
				comment = PATTERN_SPACE
						.matcher(comment).replaceAll(" ")
						.replace("\n", "");
//				System.out.println(comment);
				return comment.contains(APACHE_LICENSE_HEADER) 
						|| (PATTERN_LICENSED_UNDER.matcher(comment).find() 
								&& comment.contains(APACHE_ENDING));
			}

		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return false;
	}

	static public class CommentParserState {
		private static final int F = 9;
		private StringBuffer string;
		private char state;
		private static final char[][] table  = {
			{1,0,0,0,0},
			{2,5,0,0,0},
			{2,2,8,2,2},
			{4,F,F,3,F},
			{2,F,F,F,F},
			{5,6,5,5,5},
			{7,5,5,5,5},
			{F,F,F,F,F},
			{4,F,8,3,F},
		};

		public CommentParserState(){
			string = new StringBuffer();
			state = 0;
		}

		private char charIndex(char c){
			switch (c) {
			case '/':
				return 0;
			case '*':
				return 1;
			case '\n':
				return 2;
			case ' ':
			case '\t':
				return 3;
			default:
				return 4;
			}
		}

		public boolean next(char c){
			if (state != F){
				state = table[state][charIndex(c)];
				if (state > 0 
						&& state < F
						&& state != 3)
					string.append(c);
			}
			return state != F;
		}

		public String getString(){
			return string.toString();
		}

	}

}
