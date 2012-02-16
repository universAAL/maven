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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.DirectoryWalker;

public class SourceExplorer extends DirectoryWalker {


	private SourceChecker sc;
	
	public SourceExplorer(SourceChecker sourceTest) {
		sc = sourceTest;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean handleDirectory(File directory, int depth,
			Collection results) throws IOException {
		return !directory.getName().matches(".*\\.svn");
	}

	public ArrayList<File> walk(String startDir) {
		ArrayList<File> conflicted = new ArrayList<File>();
		try {
			this.walk(new File(startDir), conflicted);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return conflicted;

	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void handleFile(File file, int depth, Collection results)
			throws IOException {
		// System.out.println("testing: " + file.getAbsolutePath());
		if (file.getName().endsWith("java") && sc.passesTest(file)) {
			results.add(file);
		}
	}
}