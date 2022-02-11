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

import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.ontologyTools.OntologyProperty;

public class OntologyClass extends AnnotatableElement {

	private OntologyName name = null;
	// a given class could have more than one parent. 
	private ArrayList<OntologyName> parentNames = new ArrayList<OntologyName>();
	private ArrayList<OntologyProperty> properties = new ArrayList<OntologyProperty>();
	// if we wanted, for sparql gen without an oInfo, we could store to json for nodegroup:
	// private boolean hasSubClasses
	
	public OntologyClass(String name, ArrayList<String> parentNames) throws Exception {
		
		// blank nodes from SADL don't seem to always be of type class so they'll blow things up
		// Load queries work around blank nodes
		// SparqlConnection domain should exclude them.
		if (name.startsWith("nodeID://")) {
			throw new Exception("Attempting to load a blank node class: " + name);
		}
		this.name = new OntologyName(name);
		// add the parent(s)
		if(parentNames != null){
			for(String nextName : parentNames){
				this.parentNames.add(new OntologyName(nextName));
			}
		}
	}
	
	public OntologyClass(String name) throws Exception {
		this(name, null);
	}
	
	public OntologyClass(String name, boolean noChecks) {
		this.name = new OntologyName(name);
	}

	public String getName() {
		return this.name.getFullName();
	}
	
	public String getNameString(Boolean stripNamespace){
		if(stripNamespace){
			return this.name.getLocalName();
		}
		else{
			return this.name.getFullName();
		}
	}
	
	public void addParentName(String parent){
		this.parentNames.add(new OntologyName(parent));
	}
	
	public ArrayList<String> getParentNameStrings(Boolean stripNamespace){
		ArrayList<String> retval = new ArrayList<String>();
		if(stripNamespace){
			for(OntologyName pn : this.parentNames){
				retval.add(pn.getLocalName());
			}
		}
		else{
			for(OntologyName pn : this.parentNames){
				retval.add(pn.getFullName());
			}
		}
		return retval;
	}
	
	public String getNamespaceString(){
		return this.name.getNamespace();
	}
	
	public ArrayList<OntologyProperty> getProperties(){
		return this.properties;
	}
	
	public OntologyProperty getProperty(String propertyName){
		OntologyProperty retval = null;
		// find it, if we can
		for(OntologyProperty op : this.properties){
			if(op.getNameStr(false).equalsIgnoreCase(propertyName)){
				retval = op;
				break;
			}
		}
		return retval;
	}
	
	public void addProperty(OntologyProperty op){
		this.properties.add(op);
	}
	
	public Boolean equals(OntologyClass oc){
		return this.name.equals(oc.name);
	}
	
	public Boolean powerMatch(String pattern){
		String pat = pattern.toLowerCase();
		Boolean retval = this.getNameString(true).toLowerCase().contains(pat);
		return retval;
	}
	
}
