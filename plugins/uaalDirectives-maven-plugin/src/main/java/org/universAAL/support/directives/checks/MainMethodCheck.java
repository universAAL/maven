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
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.universAAL.support.directives.api.APICheck;
import org.universAAL.support.directives.util.SourceChecker;
import org.universAAL.support.directives.util.SourceExplorer;

/**
 * @author amedrano
 *
 */
public class MainMethodCheck implements SourceChecker, APICheck {

	public static String MAIN_REGEXP = "(public\\s+)?static\\s+(public\\s+)?void\\s+main\\s*\\(\\s*String(\\s*\\[\\])?\\s+\\w+(\\s*\\[\\])?\\s*\\)";

	private static Pattern MAIN_PATTERN = Pattern.compile(MAIN_REGEXP);

	/** {@ inheritDoc} */
	public boolean check(MavenProject mavenproject, Log log) throws MojoExecutionException, MojoFailureException {
		SourceExplorer se = new SourceExplorer(this);
		ArrayList<File> conflicted = se.walk(mavenproject.getBasedir() + "/src/main/java/");
		if (conflicted.size() > 0) {
			String m = "The following Files contain a main method:\n";
			for (java.util.Iterator<File> iterator = conflicted.iterator(); iterator.hasNext();) {
				m += "\t" + iterator.next().getAbsolutePath() + "\n";
			}
			m += "There should not be any main method in a Library. Consider moving this code to a Junit Test.";
			throw new MojoFailureException(m);
		}
		return true;
	}

	/** {@ inheritDoc} */
	public boolean passesTest(File sourceFile) {
		try {
			String code = FileUtils.readFileToString(sourceFile);
			code = new CommentRemoverStateMachine().removeComments(code);
			Matcher m = MAIN_PATTERN.matcher(code);
			return !m.find();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static class CommentRemoverStateMachine {

		int state = 0;

		int nextState(char c) {
			switch (state) {
			case 0:
				if (c == '/')
					return 1;
				else
					return 0;
			case 1:
				if (c == '/')
					return 2;
				else if (c == '*')
					return 3;
				else
					return 0;
			case 2:
				if (c == '\n')
					return 0;
				else
					return 2;
			case 3:
				if (c == '*')
					return 4;
				else
					return 3;
			case 4:
				if (c == '/')
					return 5;
				else if (c == '*')
					return 4;
				else
					return 3;
			case 5:
				return 0;
			default:
				break;
			}
			return 0;
		}

		String removeComments(String s) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < s.length(); i++) {
				state = nextState(s.charAt(i));
				if (state == 0) {
					sb.append(s.charAt(i));
				}
			}
			return sb.toString();
		}

	}

}
