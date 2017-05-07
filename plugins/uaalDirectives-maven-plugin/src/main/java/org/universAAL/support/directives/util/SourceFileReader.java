/*******************************************************************************
 * Copyright 2017 2011 Universidad Politécnica de Madrid
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * @author amedrano
 *
 */
public class SourceFileReader {

	public static ArrayList<String> readImports(File f) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(f));
			return SourceFileReader.lookForLinesWith(br, ".*import.*");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ArrayList<String> lookForLinesWith(BufferedReader f, String regExp) {
		ArrayList<String> matches = new ArrayList<String>();
		String s;
		try {
			while ((s = f.readLine()) != null) {
				if (s.matches(regExp)) {
					matches.add(s);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return matches;
	}

	public static String readPackage(File f) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(f));
			ArrayList<String> r = lookForLinesWith(br, ".*package.*");
			br.close();
			if (r.size() > 0) {
				return r.get(0);
			} else {
				System.out.println("no package found for " + f.getName());
				System.out.flush();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
