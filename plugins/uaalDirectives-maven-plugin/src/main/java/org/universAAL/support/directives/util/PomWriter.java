/*******************************************************************************
 * Copyright 2011 Universidad Politécnica de Madrid
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
package org.universAAL.support.directives.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author amedrano
 *
 */
public class PomWriter {

	PomFixer fixer;
	MavenProject pom;
	
	public PomWriter(PomFixer pf, MavenProject mp) {
		fixer = pf;
		pom = mp;
	}
	
	public void fix() throws Exception{
			// Reading
			Model model = readPOMFile(pom);

			// Editing
			fixer.fix(model);

			// Writing
			MavenXpp3Writer writer = new MavenXpp3Writer();

			writer.write(new OutputStreamWriter(new FileOutputStream(new File(
					pom.getFile().getAbsolutePath()))), model);
	}
	
	public static Model readPOMFile(MavenProject pom) throws FileNotFoundException, IOException, XmlPullParserException {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = reader.read(new FileInputStream(new File(pom
				.getFile().getAbsolutePath())));
		return model;
	}
}
