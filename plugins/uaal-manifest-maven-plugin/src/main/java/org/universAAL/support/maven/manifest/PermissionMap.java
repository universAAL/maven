/*
	Copyright 2007-2014 Fraunhofer IGD, http://www.igd.fraunhofer.de
	Fraunhofer-Gesellschaft - Institute for Computer Graphics Research

	See the NOTICE file distributed with this work for additional
	information regarding copyright ownership

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	  http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */
package org.universAAL.support.maven.manifest;

import java.util.ArrayList;
import java.util.HashMap;

public class PermissionMap extends HashMap<String, ArrayList<Permission>> {
	private static final long serialVersionUID = 1L;

	public void add(String busName, String typeName, Permission p) {
		String key = busName + "-" + typeName;

		ArrayList<Permission> lst;
		lst = get(key);
		if (lst == null) {
			lst = new ArrayList<Permission>();
			put(key, lst);
		}

		lst.add(p);
	}

	public void add(PermissionMap el) {
		for (String key : el.keySet()) {
			ArrayList<Permission> lst = get(key);
			if (lst == null) {
				lst = new ArrayList<Permission>();
				put(key, lst);
			}

			lst.addAll(el.get(key));
		}
	}

	public int getPermissionCount() {
		int cnt = 0;
		for (String key : keySet()) {
			ArrayList<Permission> lst = get(key);
			cnt += lst.size();
		}
		return cnt;
	}

	public String toString() {
		String s = "" + getPermissionCount() + " permissions";
		if (keySet().size() != 0)
			s += ":";
		for (String key : keySet()) {
			ArrayList<Permission> lst = get(key);
			s += "\n   " + key + ": " + lst.size();
		}

		return s;
	}
}
