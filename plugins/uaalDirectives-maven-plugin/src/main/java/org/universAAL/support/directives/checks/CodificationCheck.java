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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
public class CodificationCheck implements APICheck, SourceChecker {

	CharsetEncoder UTF8Encoder =
		      Charset.forName("UTF8").newEncoder()
		      	.onMalformedInput(CodingErrorAction.REPORT)
		      	.onUnmappableCharacter(CodingErrorAction.REPORT);

	Map<File, Integer>  lineMap = new HashMap<File, Integer>();

	/** {@ inheritDoc}	 */
	public boolean passesTest(File sourceFile) {
		// TODO Auto-generated method stub
		int lineNo = 1;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(sourceFile));
			String line = br.readLine();
			while (line!= null){
				UTF8Encoder.encode(CharBuffer.wrap(line));
				line = br.readLine();
				lineNo++;
			}
			br.close();
		} catch (FileNotFoundException e) {
			// improbable
			return false;
		} catch (CharacterCodingException e) {
			lineMap.put(sourceFile, new Integer(lineNo));
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/** {@ inheritDoc}	 */
	public boolean check(MavenProject mavenProject, Log log)
			throws MojoExecutionException, MojoFailureException {
		SourceExplorer se = new SourceExplorer(this);
		ArrayList<File> conflicted = se.walk(mavenProject.getBasedir().getAbsolutePath()
				+ "/src/main/java/");
		if (conflicted.size() > 0) {
			String m = "The following Files Contain a possibly charset coding error:\n";
			for (File file : conflicted) {
				m += "\t" + file.getAbsolutePath() + ":" + lineMap.get(file) + "\n";
			}
			m += "To solve this problem, make sure there are no charracters that can be code incompatible, or write everything in UTF-8.";
			throw new MojoFailureException(m);
		}
		return true;
	}
}
