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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ManifestReader {
	private File uaalManifest;
	private PermissionMap map = new PermissionMap();
	private XPath xpath = XPathFactory.newInstance().newXPath();

	public ManifestReader(String filename) {
		uaalManifest = new File(filename);
	}

	public ManifestReader(File file) {
		uaalManifest = file;
	}

	public PermissionMap getResult() {
		return map;
	}

	/**
	 * Reads the File {@link #uaalManifest} as xml and parses it to
	 * {@link Document} using JAXP with a {@link DocumentBuilder}.
	 *
	 * @return the parsed {@link #uaalManifest} File
	 */
	public void read() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document manifest = null;
		try {
			builder = factory.newDocumentBuilder();
			manifest = builder.parse(uaalManifest);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Node root = null;
		try {
			root = (Node) xpath.evaluate("application/permissions", manifest, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if (root == null)
			return;

		NodeList busChildren = root.getChildNodes();
		for (int i = 0; i < busChildren.getLength(); i++) {
			Node busChild = busChildren.item(i);
			if (busChild.getNodeType() == Node.ELEMENT_NODE) {
				String busName = busChild.getNodeName();
				busName = busName.replace("-", "_");
				busName = busName.replace(":", "_");
				busName = busName.replace(".", "_");
				NodeList typeChildren = busChild.getChildNodes();
				for (int j = 0; j < typeChildren.getLength(); j++) {
					Node typeChild = typeChildren.item(j);
					if (typeChild.getNodeType() == Node.ELEMENT_NODE) {
						String typeName = typeChild.getNodeName();
						typeName = typeName.replace("-", "_");
						typeName = typeName.replace(":", "_");
						typeName = typeName.replace(".", "_");
						readElement(busName, typeName, typeChild);
					}
				}
			}
		}
	}

	private void readElement(String busName, String typeName, Node root) {
		Permission p = new Permission();
		p.title = extractPermissionProperty("title", root);
		p.description = extractPermissionProperty("description", root);
		p.serialization = extractPermissionProperty("serialization", root);

		if (!p.hasNull())
			map.add(busName, typeName, p);

		// System.out.println(" --- busName: " + busName + "\t\ttypeName: "
		// + typeName + "\t\tval: " + p.title + "\n" + p.serialization);
	}

	/**
	 * Extracts a single Property of a {@link Permission}.
	 *
	 * @param property
	 *            the Property to be extracted
	 * @param from
	 *            the Entry representing a Permission the Property should be
	 *            extracted from
	 * @return the extracted Property
	 */
	private String extractPermissionProperty(String property, Node from) {
		try {
			return xpath.evaluate("normalize-space(" + property + ")", from);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
}
