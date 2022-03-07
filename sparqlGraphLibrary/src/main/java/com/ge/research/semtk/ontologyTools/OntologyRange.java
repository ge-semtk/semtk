/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */


package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class OntologyRange {
	public static final String CLASS = "http://www.w3.org/2002/07/owl#Class";
	// Complex range:  range is a set of Uris
	private HashSet<String> uris = new HashSet<String>();
	
	public OntologyRange(String fullName) {
		this.uris.add((fullName == null || fullName.isEmpty()) ? CLASS : fullName);
	}
	
	public OntologyRange deepCopy() {
		OntologyRange copy = null;
		for (String name : this.uris) {
			if (copy == null) {
				copy = new OntologyRange(name);
			} else {
				copy.addRange(name);
			}
		}
		return copy;
	}
	
	public void addRange(String name) {
		this.uris.add(name);
	}
	
	public boolean containsDefaultClass() {
		return this.uris.contains(CLASS);
	}
	
	/**
	 * Does this range have multiple values
	 * @return
	 */
	public boolean isComplex() {
		return this.uris.size() > 1;
	}
	
	public HashSet<String> getUriList() {
		return this.uris;
	}

	public boolean containsUri(String rangeURI) {
		return this.uris.contains(rangeURI);
	}
	
	public void removeUri(String rangeURI) {
		if (this.containsUri(rangeURI)) {
			this.uris.remove(rangeURI);
		}
	}
	
	public boolean equalsUri(String rangeURI) {
		return this.uris.size() == 1 && this.uris.contains(rangeURI);
	}
	/**
	 * Get name of the single simple range
	 * @return
	 * @throws Exception if this.isComplex()
	 */
	public String getSimpleUri() throws Exception {
		if (this.uris.size() > 1) {
			throw new Exception("Internal error: can not get simple name of a complex range");
		}
		for (String ret : this.uris) {
			return ret;
		}
		return "";
	}

	public String getDisplayString(boolean abbreviate) {
		return OntologyRange.getDisplayString(this.uris, abbreviate);
	}
	
	public static String getDisplayString(String classUri, boolean abbreviate) {
        HashSet<String> uriSet = new HashSet<String>();
        uriSet.add(classUri);
        return OntologyRange.getDisplayString(uriSet, abbreviate);
    }
	
	public static String getDisplayString(Collection<String> uris, boolean abbreviate) {
		if (abbreviate) {
			HashSet<String> modified = new HashSet<String>();
			for (String uri : uris) {
				modified.add(new OntologyName(uri).getLocalName());
			}
			if (modified.size() == 1) {
				return String.join("", modified);
			} else {
				return "{" + String.join(" or ", modified) + "}";
			}
		} else {
			if (uris.size() == 1) {
				return String.join("", uris);
			} else {
				return "{" + String.join(" or ", uris) + "}";
			}
		}
	}
	
	/**
	 * Reverse engineer a display string to a HashSet of uris
	 * @param str
	 * @return
	 */
	public static HashSet<String> parseDisplayString(String str) {
		HashSet<String> ret = new HashSet<String>();
		if (! str.contains("{")) {
			ret.add(str);
		} else {
			String str1 = str.replaceAll("[{}]", "").trim();
			for (String uri : str1.split(" or ")) {
				ret.add(uri);
			}
		}
		return ret;
	}
}
