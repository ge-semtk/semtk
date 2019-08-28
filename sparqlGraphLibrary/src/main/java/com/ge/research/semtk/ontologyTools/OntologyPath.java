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
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.ontologyTools.Triple;
import com.google.gson.JsonObject;

public class OntologyPath {

	private ArrayList<Triple> tripleList = new ArrayList<Triple>();
	private String startClassName = "";
	private String endClassName = "";
	
	private HashMap<String, String> classHash = new HashMap<String, String>();
	
	
	public OntologyPath(){
		// do nothing.
	}
	
	public OntologyPath(String startClassname){
		this.classHash.put(startClassname, "1");
		this.startClassName = startClassname;
		this.endClassName = startClassname;
	}
	
	public void addJsonTriples(JSONArray j) throws Exception {
		
		for (Object o : j) {
			JSONArray arr = (JSONArray) o;
			this.addTriple((String)arr.get(0), (String)arr.get(1), (String)arr.get(2));
		}
	}
	
	public void addTriple(String className0, String attributename, String className1) throws PathException{
		// add whichever class is new to the hash and endpoint. 
		if(className0.equalsIgnoreCase(this.endClassName)){
			this.classHash.put(className1, "1");
			this.endClassName = className1;
		}
		else if(className1.equalsIgnoreCase(this.endClassName)){
			this.classHash.put(className0, "1");
			this.endClassName = className0;	
		}
	
		else{
			throw new PathException("OntologyPath.addTriple() : Error adding triple to path. It is not connected. Triple was: "+ className0 + ", " + attributename + ", " + className1);
		}
		
		// add triple to the end of the path.
		this.tripleList.add(new Triple(className0, attributename, className1));
	}
	
	@Deprecated
	public ArrayList<Triple> getAsList(){
		return this.tripleList;
	}
	
	public ArrayList<Triple> getTripleList(){
		return this.tripleList;
	}
	
	
	// asOldFashionedList should be deprecated
	
	public String getClass0Name(int tripleIndex){
		return this.tripleList.get(tripleIndex).getSubject();
	}
	
	public String getClass1Name(int tripleIndex){
		return this.tripleList.get(tripleIndex).getObject();
	}
	
	public String getStartClassName() {
		return this.startClassName;
	}
	
	// for paul
	public String getEndClassName() {
		return this.endClassName;
	}
	
	// for paul
	public String getAnchorClassName() {
		return this.endClassName;
	}
	
	public String getAttributeName(int tripleIndex){
		return this.tripleList.get(tripleIndex).getPredicate();
	}
	
	public Triple getTriple(int tripleIndex){
		return this.tripleList.get(tripleIndex);
	}
	
	public int getLength(){
		return this.tripleList.size();
	}
	
	public boolean containsClass(String classToCheck){
		boolean retval = false;
		if(this.classHash.containsKey(classToCheck)){
			retval = true;
		}
		
		return retval;
	}
	
	public boolean isSingleLoop() {
		boolean retval = false;
		// if there is only one entry and the subject and object match, then we have a small closed loop. 
		if(this.tripleList.size() == 1 && this.tripleList.get(0).getSubject() ==  this.tripleList.get(0).getObject() ){
			retval = true;
		}
		
		return retval;
	}
	
	public OntologyPath deepCopy() throws PathException{
		OntologyPath retval = new OntologyPath(this.startClassName);
		
		// add each of the triples as a copy to the returned Path
		for(Triple t : this.tripleList){
			retval.addTriple(t.getSubject(), t.getPredicate(), t.getObject());
		}
		
		return retval;
	}
	
	public String debugString() {
		String ret = "(OntologyPath from " + this.getStartClassName() + " to " + this.getEndClassName() + ") \n [";
		for (int i=0; i < this.tripleList.size(); i++) {
			Triple t = this.tripleList.get(i);
			ret = ret + "   [" + new OntologyName(t.getSubject()).getLocalName() + ", " + new OntologyName(t.getPredicate()).getLocalName() + ", " + new OntologyName(t.getObject()).getLocalName() + "],\n  ";
		}
		ret = ret + "]";
		return ret;
	}
	
	public String asString() {
		// generate a one-line string for the user to choose or select paths
		String ret = "";
		for (int i=0; i < this.tripleList.size(); i++) {
			
			String from =  new OntologyName(this.tripleList.get(i).getSubject()).getLocalName();
			String via =  new OntologyName(this.tripleList.get(i).getPredicate()).getLocalName();
			String to =  new OntologyName(this.tripleList.get(i).getObject()).getLocalName();
			
			// Always show first class and attribute
			ret += from + "." + via + " ";
			
			// If "to" does not equal first class in next triple then put it in too
			if (i == this.tripleList.size() - 1 || ! to.equals(new OntologyName(this.tripleList.get(i+1).getObject()).getLocalName())) {
				ret += to + " ";
			}
		}
		
		return ret;
	}
	
	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		JSONArray triples = new JSONArray();
		for (Triple t : this.tripleList) {
			triples.add(t.toCsvString());
		}
		ret.put("triples", triples);
		return ret;
	}
}
