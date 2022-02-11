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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.ontologyTools.OntologyRange;

/** 
 * Represents Datatype or Object Property
 * @author 200001934
 *
 */
public class OntologyProperty extends AnnotatableElement{

	private OntologyName  name = null;
	private Hashtable<String, OntologyRange> rangeHash = new Hashtable<String,OntologyRange>();
	

	public OntologyProperty(String name, String domain, String range){
		this.name  = new OntologyName(name);
		this.rangeHash.put(domain, new OntologyRange(range));
	}
	
	public OntologyName getName() {
		return this.name;
	}
	
	// copy another property except for the name
	public OntologyProperty(String name, OntologyProperty toCopy) {
		this.name  = new OntologyName(name);
		for (String key : toCopy.rangeHash.keySet()) {
			this.rangeHash.put(key, toCopy.rangeHash.get(key).deepCopy());
		}
	}
	/**
	 * Get range of property given the domain.
	 * @param domain 
	 * @param oInfo
	 * @return
	 * @throws Exception - doesn't exist
	 */
	public OntologyRange getRange(OntologyClass domain, OntologyInfo oInfo) throws Exception {
		
		OntologyRange ret = this.rangeHash.get(domain.getNameString(false));
		if (ret != null) {
			return ret;
		} else if (domain != null) {
			// try parents breadth-first
			for (String c : oInfo.getClassAncestorNames(domain)) {
				ret = this.rangeHash.get(c);
				if (ret != null) {
					return ret;
				}
			}
		} 
		
		throw new Exception("Can not find range of property: " + this.getNameStr(false) + " for domain: " + domain.getNameString(false));
	}
	
	public Set<String> getRangeDomains() {
		return this.rangeHash.keySet();
	}
	public void setRange(OntologyClass domain, OntologyRange range) {
		this.rangeHash.put(domain.getNameString(false), range);
	}
	public void removeRange(OntologyClass domain) {
		this.rangeHash.remove(domain.getNameString(false));
	}
	public void removeRange(String domain) {
		this.rangeHash.remove(domain);
	}
	public void removeFromRange(String domain, String uri) {
		OntologyRange oRange = this.rangeHash.get(domain);
		oRange.removeUri(uri);
	}
	
	/**
	 * Add a Uri to a Range at the given domain.
	 * @param domain
	 * @param rangeUri
	 */
	public void addRange(OntologyClass domain, String rangeUri) {
		String domainUri = domain.getNameString(false);
		if (this.rangeHash.containsKey(domainUri)) {
			this.rangeHash.get(domainUri).addRange(rangeUri);
		} else {
			this.rangeHash.put(domainUri, new OntologyRange(rangeUri));
		}
	}
	
	public String getNameStr() {
		return this.getNameStr(false);
	}
	public String getNameStr(Boolean stripNamespace){
		if(stripNamespace){
			return this.name.getLocalName();
		}
		else{	// don't strip namespace
			return this.name.getFullName();
		}
	}
	
	
	
}
