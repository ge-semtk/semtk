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
import java.util.Arrays;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

/**
 * OntologyInfo is a class that contains the bulk of the understanding of the actual model.
 * it represents all the classes, relationships between them (subclass relations) and related
 * properties. 
 * it provides functionality for determining pathing between two arbitrary classes, enumeration
 * values, and types for properties.
 **/
public class OntologyInfo {

	// indexed list of all the classes in the ontology of interst
	private HashMap<String, OntologyClass> classHash = new HashMap<String, OntologyClass>();
	// indexed list of all of the properties in the ontoogy of interest
	private HashMap<String, OntologyProperty> propertyHash = new HashMap<String, OntologyProperty>();
	// for each class, the collection of valid, single-hop paths to and from other classes. 
	private HashMap<String, ArrayList<OntologyPath>> connHash = new HashMap<String, ArrayList<OntologyPath>>();
	// for each class, list its subclasses. the superclasses are sored in the class object itself. 
	private HashMap<String, ArrayList<OntologyClass>> subclassHash = new HashMap<String, ArrayList<OntologyClass>>();
	// a list of all the enumerations available for a given class. these are handled as full uris for convenience sake. 
	private HashMap<String, ArrayList<String>> enumerationHash = new HashMap<String, ArrayList<String>>();
	
	private ArrayList<String> pathWarnings = new ArrayList<String>();  // problems incurred searching for a path.	

	private final static int MAXPATHLENGTH = 50;	// how many hops, max, allowed in a returned path between arbitrary nodes
	
	private static int restCount = 0;
	/**
	 * Default constructor
	 */
	public OntologyInfo(){
	}

	/**
	 * Constructor that also loads oInfo
	 */
	public OntologyInfo(SparqlEndpointInterface endpoint, String domain) throws Exception{
		load(endpoint, domain); 
	}
	
	/**
	 * add a new class to the ontology info object. this includes information on super/sub classes
	 * being added to a hash of the known entities.
	 **/
	public void addClass(OntologyClass oClass){
		String classnameStr = oClass.getNameString(false);	// get the full name of the class and do not strip URI info.
		this.connHash.clear(); // TODO: ask Paul why this is cleared whenever a class is added. 
		
		this.classHash.put(classnameStr, oClass);	// silently overwrites if the class is already present.
		// store info on the related subclasses
		ArrayList<String> superClassNames = oClass.getParentNameStrings(false); // get the parents. there may be more than one. 
		// spin through the list and find the ones that need to be added.
		for(String scn : superClassNames){
			if(!(this.subclassHash.containsKey(scn))){
				// the superclass was not previously added.
				ArrayList<OntologyClass> scList = new ArrayList<OntologyClass>();
				scList.add(oClass);
				this.subclassHash.put(scn, scList);
			}
			else{
				// add this value
				this.subclassHash.get(scn).add(oClass);
			}
		}
	}
	
	public ArrayList<String> getSubclassNames(String superClassName) {
		return this.getSubclassNames(superClassName, null);
	}
	/**
	 * return a list of subclass names for a given class.
	 * if there are no known subclasses, an empty list is returned.
	 **/
	public ArrayList<String> getSubclassNames(String superClassName, ArrayList<String> retval){
		// return an arraylist of the subclasses, if any, of the given class.
		if(retval == null){	retval = new ArrayList<String>(); } // if it was null, initialize it.
		
		ArrayList<OntologyClass> subclasses = this.subclassHash.get(superClassName);
	
		if (subclasses != null) {
			for(OntologyClass currSubclass : subclasses){
				retval.add(currSubclass.getNameString(false));	// check the existing one and add it
				retval = this.getSubclassNames(currSubclass.getNameString(false), retval);  // recursively add the subclasses
			}
		}
		// return the list so far.
		return retval;
	}
	
	public ArrayList<String> getSuperclassNames(String superClassName) {
		return this.getSuperclassNames(superClassName, null);
	}
	/**
	 * return a list of the superclasses for a given class.
	 * if there are no known super classes, an empty list is returned.
	 **/
	public ArrayList<String> getSuperclassNames(String subclassName, ArrayList<String> retval){
		// return an arraylist of the superclasses, if any, for a given class
		// the current implementation may return multiple entries for the same value.
		
		if(retval == null){ retval = new ArrayList<String>(); } // if it was null, initialize it.
		
		ArrayList<OntologyClass> superclasses = new ArrayList<OntologyClass>();
		for(String currParentName : this.classHash.get(subclassName).getParentNameStrings(false)){
			retval.add(currParentName);
			superclasses.add(this.classHash.get(currParentName));
		}
		
		// get the Parents' parents.
		for(OntologyClass currParentClass : superclasses){
			retval.add(currParentClass.getNameString(false));
			retval = this.getSuperclassNames(currParentClass.getNameString(false), retval);
		}
		// ship out the results so far gathered. 
		return retval;
	}
	
	public ArrayList<String> getPathWarnings() {
		return pathWarnings;
	}
	
	public ArrayList<String> getPropNames() {
		return new ArrayList<String>(this.propertyHash.keySet());
	}
	
	public ArrayList<OntologyPath> getConnList(String classNameStr) throws ClassException, PathException {
		// return or calculate all legal one-hop path connections to and from a class
		
		if (! this.connHash.containsKey(classNameStr)) {
			ArrayList<OntologyPath> ret = new ArrayList<OntologyPath>();
			// OntologyProperty prop; // bug in Javascript that doesn't do anything bad
			OntologyPath path;
			if (! this.classHash.containsKey(classNameStr)) {
				throw new ClassException("Internal error in OntologyInfo.getConnList(): class name is not in the ontology: " + classNameStr);
			}
			OntologyClass classVal = this.classHash.get(classNameStr);
			HashMap <String, Integer> foundHash = new HashMap <String, Integer>();     // hash of path.asString()     PEC TODO FAILS when Man-hasSon->Man hashes same as Man<-hasSon-Man
			String hashStr = "";
			
			
			//--- calculate HasA:   exact range classes for all inherited properties
			ArrayList <OntologyProperty> props = this.getInheritedProperties(classVal);
			for (int i=0; i < props.size(); i++) {
				OntologyProperty prop = props.get(i);
				String rangeClassName = prop.getRangeStr();
				
				// if the range class in this domain
				if (this.containsClass(rangeClassName)) {
					
					// Exact match:  class -> hasA -> rangeClassName
					path = new OntologyPath(classNameStr);
					path.addTriple(classNameStr, prop.getNameStr(), rangeClassName);
					hashStr = path.asString();
					if (! foundHash.containsKey(hashStr)) {
						ret.add(path);
						foundHash.put(hashStr, 1);
					}
				
					// Sub-classes:  class -> hasA -> subclass(rangeClassName)
					ArrayList<String> rangeSubNames = this.getSubclassNames(rangeClassName);
					for (int j=0; j < rangeSubNames.size(); j++) {
						if (this.containsClass(rangeSubNames.get(j))) {
							path = new OntologyPath(classNameStr);
							path.addTriple(classNameStr, prop.getNameStr(), rangeSubNames.get(j));
							hashStr = path.asString();
							if (! foundHash.containsKey(hashStr)) {
								ret.add(path);
								foundHash.put(hashStr, 1);
							}
						}
					}
				}
			}
			
			//--- calculate HadBy: class which HasA classNameStr
			
			// store all superclasses of target class
			ArrayList<String> supList = this.getSuperclassNames(classNameStr);
			
			// loop through every single class in oInfo
			for (String cname : this.classHash.keySet() ) {
				
				// loop through every property
				// Issue 50 : fixed this to get inherited properties
				// var cprops = this.classHash[cname].getProperties();
				ArrayList<OntologyProperty> cprops = this.getInheritedProperties(this.classHash.get(cname));
				
				for (int i=0; i < cprops.size(); i++) {
					OntologyProperty prop = cprops.get(i);
					String rangeClassStr = prop.getRangeStr();
					
					// HadBy:  cName -> hasA -> class
					if (rangeClassStr.equals(classNameStr)) {
						path = new OntologyPath(classNameStr);
						path.addTriple(cname, prop.getNameStr(), classNameStr);
						hashStr = path.asString();
						if (! foundHash.containsKey(hashStr)) {
							ret.add(path);
							foundHash.put(hashStr, 1);
						}
					}
					
					// IsA + HadBy:   cName -> hasA -> superClass(class)
					for (int j = 0; j < supList.size(); j++) {
						if (rangeClassStr.equals(supList.get(j))) {
							path = new OntologyPath(classNameStr);
							path.addTriple(cname, prop.getNameStr(), classNameStr);
							hashStr = path.asString();
							if (! foundHash.containsKey(hashStr)) {
								ret.add(path);
								foundHash.put(hashStr, 1);
							}
						}
					}
				}
			}
			this.connHash.put(classNameStr, ret);
		}
		
		return this.connHash.get(classNameStr);
	}
	/**
	 * Return a list of all the classes that are not in the range of some property.
	 * these would always appear on the left side of a tuple (S, P, O) (except where another class is
	 * listed as being a subtype of this class)
	 **/
	public ArrayList<String> getDomainRangeRoots() {
		// get the full names of all classes not in the range of other classes
		ArrayList<String> retval = new ArrayList<String>();
		// add all of the class names to the return list so we start with all of them
		for(String nm : this.classHash.keySet()){ 
			retval.add(nm);
		}
	
		// loop through all the classes
		for(String currKey : this.classHash.keySet()){
			OntologyClass oClass = this.classHash.get(currKey);
			ArrayList<OntologyProperty> props = oClass.getProperties();
			// loop through the exact properties. (inherited ones will appear eventually)
			for(OntologyProperty currProp : props){
				// the property seems to have only a single range at current. this is represented here as well
				ArrayList<String> rangeClasses = new ArrayList<String>();
				rangeClasses.add(currProp.getRangeStr());
				ArrayList<String> tempSubList = null;
				for(String sClassName : this.getSubclassNames(currProp.getRangeStr(), tempSubList)){
					rangeClasses.add(sClassName); // add the names.
				}
				
				// remove the range and its subclasses from the return list
				for(String rClass : rangeClasses){
					retval.remove(rClass);	// pull this from the return list. 
				}
			}
		}
		// return the domain roots
		return retval;
	}
	/**
	 * returns the count of known classes.
	 **/
	public int getNumberOfClasses(){
		// how many classes do we know of at all?
		return this.classHash.size();
	}
	
	/**
	 * Returns an instance of OntologyClass for a given URI. 
	 * if the OntologyInfo object has no entry for the URI, null is returned.
	 **/
	public OntologyClass getClass(String fullUriName){
		// get the requested class
		OntologyClass retval = null;
		if(this.classHash.containsKey(fullUriName)){ retval = this.classHash.get(fullUriName); }
		return retval;
	}
	
	/**
	 * return all the known class names
	 **/
	public ArrayList<String> getClassNames(){
		// return the names of all classes we know of
		ArrayList<String> retval = new ArrayList<String>();
		retval.addAll(this.classHash.keySet());
		return retval;
	}
	
	/**
	 * for a given class, return all of the known parent classes
	 **/
	public ArrayList<OntologyClass> getClassParents(OntologyClass currentClass){
		ArrayList<OntologyClass> retval = new ArrayList<OntologyClass>();
		
		// add each parent in turn
		for(String parentNameString : currentClass.getParentNameStrings(false)){
			retval.add(this.getClass(parentNameString));
		}
		// return the group.
		return retval;
	}
	
	/**
	 * return all of the known property names.
	 **/
	public ArrayList<String> getPropertyNames(){
		ArrayList<String> retval = new ArrayList<String>();
		retval.addAll(this.propertyHash.keySet());
		return retval;
	}
	
	/**
	 * return the complete list of single-hop connections between the given class and all
	 * of the other known classes.
	 **/
	public ArrayList<String> getConnectionList(String classNameString) throws Exception{
		ArrayList<String> retval = new ArrayList<String>();
		// TODO: actual method.
		throw new Exception("getConnectionList: method not implemented.");
		
		// return retval;
	}
	/**
	 * returns true/false value of whether a class is known to the OntologyInfo object
	 **/
	public Boolean containsClass(String classNameString){
		return this.classHash.containsKey(classNameString);
	}
	
	/**
	 * returns true/false value of whether a property is known to the OntologyInfo object
	 **/
	public Boolean containsProperty(String propertynameString){
		return this.propertyHash.containsKey(propertynameString);
	}
	
	/**
	 * returns the count of enumerated types known to the OntologyInfo object
	 **/
	public int getNumberOfEnum(){
		return this.enumerationHash.size();
	}
	
	/**
	 * return count of properties known to the OntologyInfo object.
	 **/
	public int getNumberOfProperties(){
		return this.propertyHash.size();
	}
	
	/**
	 * for a given class, return all of its properties and properties it inherits.
	 **/
	public ArrayList<OntologyProperty> getInheritedProperties(OntologyClass oClass){
		ArrayList<OntologyProperty> retval = new ArrayList<OntologyProperty>();
		HashMap<String, OntologyProperty> tempRetval = new HashMap<String, OntologyProperty>();
		
		// get the full list...
		// walk up the parent chain and then add all the properties we need. 
		ArrayList<String> fullParentList = null; 
		fullParentList = this.getSuperclassNames(oClass.getNameString(false), fullParentList);
		fullParentList.add(oClass.getNameString(false));
		
		// go through the superclass list and gather all of the properties.
		for(String scn : fullParentList){
			// add each property from the super class
			for(OntologyProperty currProp : this.classHash.get(scn).getProperties()){
				tempRetval.put(currProp.getNameStr(), currProp);
			}
		}
		
		// assemble into a single list (without repeated values) and ship it out.
		Object[] keys = tempRetval.keySet().toArray();
		Arrays.sort(keys);
		
		for(Object propKey : keys){
			retval.add(tempRetval.get((String)propKey));
		}
		return retval;
	}
	
	/**
	 * for a given class, return all of the properties of itself and its decendants.
	 **/
	public ArrayList<OntologyProperty> getDescendantProperties(OntologyClass oClass){
		ArrayList<OntologyProperty> retval = new ArrayList<OntologyProperty>();
		HashMap<String, OntologyProperty> tempRetval = new HashMap<String, OntologyProperty>();
		// get the full list...
		// walk up the parent chain and then add all the properties we need. 
		ArrayList<String> fullChildList = null; 
		fullChildList = this.getSubclassNames(oClass.getNameString(false), fullChildList);
		
		// go through the superclass list and gather all of the properties.
		for(String scn : fullChildList){
			// add each property from the super class
			for(OntologyProperty currProp : this.classHash.get(scn).getProperties()){
				tempRetval.put(currProp.getNameStr(), currProp);
			}
		}
				
		// assemble into a single list (without repeated values) and ship it out.
		for(String propKey : tempRetval.keySet()){
			retval.add(tempRetval.get(propKey));
		}
		return retval;
	}
	
	public OntologyProperty getInheritedPropertyByKeyname(OntologyClass ontClass, String propName) {
		ArrayList<OntologyProperty> props = this.getInheritedProperties(ontClass);
		for (OntologyProperty i : props) {
			if (i.getNameStr(true).equals(propName)) {
				return i;
			}
		}
		return null;
	}
	
	/**
	 * returns the sparql for getting the sub and super-class relationships for known classes.
	 **/
	public static String getSuperSubClassQuery(String domain){
		// returns a very basic query 
		// domain : something like "caterham.ge.com"
		
		// TODO: inherited logic from the js rendition of this method. fix this method as that one evolves. 
		String retval = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				       	"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				       	"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + 
				       	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				       	"select distinct ?x ?y  where { " +
				       	"?x rdfs:subClassOf ?y " +
				       	" filter regex(str(?x),'^" + domain + "') " +
				        " filter regex(str(?y),'^" + domain + "') " + 
				        " filter (?x != ?y). } order by ?x";
		
		return retval;
	}

	/**
	 * process the results of the query to get all of the sub- and super-class query and loads
	 * them into the OntologyInfo object.
	 **/
	public void loadSuperSubClasses(SparqlEndpointInterface endpoint) throws Exception{
		String xList[] = endpoint.getStringResultsColumn("x");
		String yList[] = endpoint.getStringResultsColumn("y");

		HashMap<String, OntologyClass> tempClasses = new HashMap<String, OntologyClass>();
				
		for (int i=0; i < xList.length; i++) {
			// check for the existence of the current class. 
			if(!tempClasses.containsKey(xList[i])){
				OntologyClass c = new OntologyClass(xList[i], null);
				tempClasses.put(xList[i], c);
			}
			// get the current class and add the parent.
			OntologyClass c = tempClasses.get(xList[i]);
			c.addParentName(yList[i]);
		}

		// call addClass() on the temp list.
		for(String keyName : tempClasses.keySet()){
			OntologyClass oClass = tempClasses.get(keyName);
			this.addClass(oClass);
		}
		
	}
	/**
	 * returns the sparql query used to get all top-level classes of interest. these classes do not
	 * have meaningful super-classes.
	 **/
	public static String getTopLevelClassQuery(String domain){
		// domain : something like "caterham.ge.com"
		String retval = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
		       			"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
		       			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + 
		       			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
		       			"select distinct ?Class  { " +
		       			"?Class rdf:type owl:Class filter regex(str(?Class),'^" + domain + "') . " +
		       			"MINUS " +
		       			"{?Class rdfs:subClassOf ?Sup " +
		       		    "  filter regex(str(?Sup),'^" + domain + "') " +
		       			"   filter (?Class != ?Sup).} }";
		
		return retval; 
	}
	
	/**
	 * processes the results of the top-level class query. the results of this query are loaded into 
	 * the OntologyInfo object.
	 **/
	public void loadTopLevelClasses(SparqlEndpointInterface endpoint) throws Exception{
		String xList[] = endpoint.getStringResultsColumn("Class");
		
		for (int i=0; i < xList.length; i++) {
			// add it.
			OntologyClass c = new OntologyClass(xList[i], null);
			this.addClass(c);
		}
		
	}
	
	/**
	 * returns the sparql query to get all of the enumerated values found in the model.
	 **/
	public static String getEnumQuery(String domain){
		String retval = "select ?Class ?EnumVal where { " +
				"  ?Class <http://www.w3.org/2002/07/owl#equivalentClass> ?ec filter regex(str(?Class),'^" + domain + "'). " + 
				"  ?ec <http://www.w3.org/2002/07/owl#oneOf> ?c . " +
				"  ?c <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>*/<http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?EnumVal. " +
				"}";
		
		return retval;
	}
	
	/**
	 * processes the results of the enumeration query and loads the results into the enumeration hashmap
	 * in the OntologyInfo object. 
	 */
	public void loadEnums(SparqlEndpointInterface endpoint) throws Exception{
		String classList[] = endpoint.getStringResultsColumn("Class");
		String enumValList[] = endpoint.getStringResultsColumn("EnumVal");
		
		for(int i = 0; i < classList.length; i += 1){
			String className = classList[i];
			String enumVal  = enumValList[i];
			
			if(this.enumerationHash.containsKey(className)){
				this.enumerationHash.get(className).add(enumVal);
			}
			else{
				ArrayList<String> enumList = new ArrayList<String>();
				enumList.add(enumVal);
				this.enumerationHash.put(className, enumList);
			}
		}
		
		
	}
	
	/**
	 * returns the sparql query used to get all of the properties in scope.
	 */
	public static String getLoadPropertiesQuery(String domain){
		String retval = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
						"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
						"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
						"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
						"PREFIX  list: <http://jena.hpl.hp.com/ARQ/list#> " +
						"select distinct ?Class ?Property ?Range { " +
						"{" +
							"?Property rdfs:domain ?Class filter regex(str(?Class),'^" + domain + "'). " + 
							"?Property rdfs:range ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')). " +
						"} UNION {" +
							"?Property rdfs:domain ?x. " +
							"?x owl:unionOf ?y. " +
							buildListMemberSPARQL("?y", "?Class", "filter regex(str(?Class),'^" + domain + "')") +
							//"?y list:member ?Class filter regex(str(?Class),'^" + domain + "'). " +
							"?Property rdfs:range ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')). " +
						"} UNION {" +
							"?Property rdfs:domain ?Class filter regex(str(?Class),'^" + domain + "')." +
							"?Property rdfs:range ?x. " +
							"?x owl:unionOf ?y. " +
					        buildListMemberSPARQL("?y", "?Range", "filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML'))") + 
							//"?y list:member ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')). " +
						"} UNION {" +
							"?Property rdfs:domain ?x. " +
							"?x owl:unionOf ?y. " +
							buildListMemberSPARQL("?y", "?Class", "filter regex(str(?Class),'^" + domain + "')") +
							//"?y list:member ?Class filter regex(str(?Class),'^" + domain + "'). " +
							"?Property rdfs:range ?x1. " +
							"?x1 owl:unionOf ?y1. " +
					        buildListMemberSPARQL("?y1", "?Range", "filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML'))") + 
							//"?y1 list:member ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')). " +
						"} UNION {" +
							"?Class rdfs:subClassOf ?x filter regex(str(?Class),'^" + domain + "'). " +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. " +
							"?x owl:onClass ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')). " +
						"} UNION {" +
							"?Class rdfs:subClassOf ?x filter regex(str(?Class),'^" + domain + "'). " +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. ?x owl:onClass ?y. " +
							"?y owl:unionOf ?z. " +
					        buildListMemberSPARQL("?z", "?Range", "filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML'))") + 
							//"?z list:member ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')). " +
				        "} UNION {" +
							"?x1 owl:unionOf ?x2. " + 
					        buildListMemberSPARQL("?x2", "?Class", "filter regex(str(?Class),'^" + domain + "')" ) +
					        //"?x2 list:member ?Class filter regex(str(?Class),'^" + domain + "'). " +
					        "?x1 rdfs:subClassOf ?x . " +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. ?x owl:onClass ?y. " +
					        "?y owl:unionOf ?z. " +
					        buildListMemberSPARQL("?z", "?Range", "filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML'))") + 
							//"?z list:member ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')). " +
						"} UNION { " +
					        "?Class rdfs:subClassOf ?x filter regex(str(?Class),'^" + domain + "'). " +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. " +
					        "?x owl:someValuesFrom ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML'))." +
						"} UNION {" +
					        "?Class rdfs:subClassOf ?x filter regex(str(?Class),'^" + domain + "'). " +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. " +
					        "?x owl:allValuesFrom ?Range filter (regex(str(?Range),'^" + domain + "') || regex(str(?Range),'XML')).} " +
						"}";
		return retval;
	}
	
	// Ravi's original solution 12/2/2016
	private static String buildListMemberSPARQL1(String varName, String classVar, String filter) {
		restCount += 1;
		return String.format("{ {%s rdf:first %s %s.} UNION {%s rdf:rest+ ?Rest%d. ?Rest%d rdf:first %s %s.} }  ", 
							 varName, classVar, filter, varName, restCount, restCount, classVar, filter );
	}
	
	// Ravi's revised simpler solution 12/05/2016
	private static String buildListMemberSPARQL(String varName, String classVar, String filter) {
		restCount += 1;
		return String.format("{ %s rdf:rest* ?Rest%d. ?Rest%d rdf:first %s %s. }",
				varName, restCount, restCount, classVar, filter );
	}
	/**
	 * process the results of the properties sparql query and loads them into the properties hashmap
	 * in the OntologyInfo object.
	 */
	public void loadProperties(SparqlEndpointInterface endpoint) throws Exception{
		String classList[] = endpoint.getStringResultsColumn("Class");
		String propertyList[] = endpoint.getStringResultsColumn("Property");
		String rangeList[] = endpoint.getStringResultsColumn("Range");
		
		// loop through and make the property, pull class...
		for(int i = 0; i < classList.length; i += 1){
			OntologyProperty prop = new OntologyProperty(propertyList[i], rangeList[i]);
			OntologyClass c = this.classHash.get(classList[i]);
			if(c == null){
				throw new Exception("Cannot find class " + classList[i] + " in the ontology");
			}			
			c.addProperty(prop);
			
			// SemTk doesn't handle multiple ranges properly
			// Do a little bit of error handling / warning
			if (this.propertyHash.containsKey(propertyList[i]) && 
					! this.propertyHash.get(propertyList[i]).getRangeStr().equals(rangeList[i])) {
				throw new Exception(String.format("SemTk doesn't handle complex ranges.\nClass %s property domain %s\nrange 1: %s\nrange 2: %s",
												  classList[i], 
												  propertyList[i], 
												  this.propertyHash.get(propertyList[i]).getRangeStr(),
												  rangeList[i]));
			}
			
			this.propertyHash.put(propertyList[i], prop);
		}
	}

	/**
	 * Returns true/false to indicate whether the given class is a known enumeration.
	 * @param classURI
	 * @return
	 */
	public Boolean classIsEnumeration(String classURI){
		Boolean retval = false;
		if(this.enumerationHash.containsKey(classURI)){ retval = true; }
		return retval;
	}
	/**
	 * given the URI of a class one belives the Enumeration string is an instance of, the fully-qualified 
	 * name is returned. if there is no match, null is returned.
	 * @param classURI
	 * @param enumerationString
	 * @return
	 */
	public String getMatchingEnumeration(String classURI, String enumerationString){
		// return full URI of the enumeration string matching an enumerated value for classURI
		// returns Null if classURI is not enumerated or if none of its enumeration values end in enumeration string
		
		// this handles both the case where the user gives just a fragment as well as the fully qualified URI value.
		String retval = null;
		
		if(this.enumerationHash.containsKey(classURI)){
			ArrayList<String> candidates = this.enumerationHash.get(classURI);
			for(String candidate : candidates){
				// fragment passed
				if(!enumerationString.contains("#") && candidate.endsWith("#" + enumerationString )){
					// found the one we want. 
					retval = candidate;
					break;
				}
				// fully qualified URI passed.
				else if(enumerationString.contains("#") && candidate.equals(enumerationString )){
					// found the one we want. 
					retval = candidate;
					break;
				}
			}
		}
		// return an answer
		return retval;
	}
	
	public ArrayList<OntologyPath> findAllPaths(String fromClassName, ArrayList<String> targetClassNames, String domain) throws PathException, ClassException {
		//   NOTE:  lots of [sic] stuff in here so that this will match the Javascript VERY CLOSELY
		//   A form of A* path finding algorithm
		//   See getConnList() for the types of connections that are allowed
		//   Returns a list shortest to longest:  [path0, path1, path2...]
		//        pathX.getStartClassName() == fromClassName
		//        pathX.getEndClassName() == member of targetClassNames
		//        pathX.asList() returns list of triple lists [[className0, att, className1], [className1, attName, className2]...]
			
		long t0 = System.currentTimeMillis();
		this.pathWarnings = new ArrayList<String>();
		ArrayList<OntologyPath> waitingList = new ArrayList<OntologyPath>();
		waitingList.add(new OntologyPath(fromClassName));
		ArrayList<OntologyPath> ret = new ArrayList<OntologyPath>();
		HashMap<String, Integer> targetHash = new HashMap<String,Integer>(); // hash of all possible ending classes:  targetHash[className] = 1
		
		final int LENGTH_RANGE = 2;
		final int SEARCH_TIME_MSEC = 5000;
		final int LONGEST_PATH = 10;
		final boolean CONSOLE_LOG = false;
		
		/* ISSUE 50:  but perhaps these should be handled by a caller, not by this method
		 * Pathfinding should also find paths
 		 *   A) from the new class to a superclass of an existing class
         *   B) from an existing class to a superclass of the new class.
		 */
		
		// return if there is no endpoint
		if (targetClassNames.isEmpty()) { return ret; }
		
		// PEC CONFUSED:  comment placeholder 
		// so this looks
		// like the javascript
		
		// set up targetHash[targetClass] = 1
		for (int i=0; i < targetClassNames.size(); i++) {
			targetHash.put(targetClassNames.get(i), 1);
		}
		
		// STOP CRITERIA A: search as long as there is a waiting list 
		while (! waitingList.isEmpty()) {
			// pull one off waiting list
			OntologyPath item = waitingList.remove(0);
			String waitClass = item.getEndClassName();
			OntologyPath waitPath = item;
			
			// STOP CRITERIA B:  Also stop searching if:
			//    this final path (with 1 added connection) will be longer than the first (shortest) already found path
			if (!ret.isEmpty() && 
				(waitPath.getLength() + 1  > ret.get(0).getLength() + LENGTH_RANGE)) {
				break;
			} 
			
			// STOP CRITERIA C: stop if path is too long
			if (waitPath.getLength() > LONGEST_PATH) {
				break;
			}
			
			// STOP CRITERIA D: too much time spent searching
			long tt = System.currentTimeMillis();
			// PEC TODO: false && turns it off for debugging
			if (tt - t0 > SEARCH_TIME_MSEC) {
				this.pathWarnings.add("Note: Path-finding timing out.  Search incomplete.");
			}
			
			// get all one hop connections and loop through them
			ArrayList<OntologyPath> conn = this.getConnList(waitClass); 
			for (int i=0; i < conn.size(); i++) {
				
				//  each connection is a path with only one node (the 0th)
				//  grab the name of the newly found class
				String newClass = "";
				OntologyPath newPath = null;
				boolean loopFlag = false;
				
				// if the newfound class is pointed to by an attribute of one on the wait list
				if (conn.get(i).getStartClassName().equals(waitClass)) {
					newClass = conn.get(i).getEndClassName();
					
				} else {
					newClass = conn.get(i).getStartClassName();
				}
				
				// check for loops in the path before adding the class
				if (waitPath.containsClass(newClass)) {
					loopFlag = true;
				} 
				
				// build the new path
				Triple t = conn.get(i).getTriple(0);
				newPath = waitPath.deepCopy();
				newPath.addTriple(t.getSubject(), t.getPredicate(), t.getObject());
				
				// if path leads anywhere in domain, store it
				OntologyName name = new OntologyName(newClass);
				if (name.isInDomain(domain)) {
					
					// if path leads to a target, push onto the ret list
					if (targetHash.containsKey(newClass)) {
						ret.add(newPath);
						if (CONSOLE_LOG) { System.out.println(">>>found path " + newPath.debugString()); }
						
					// PEC CONFUSED: this used to happen every time without any "else" or "else if"
					
					// if path doens't lead to target, add to waiting list
					// But if it is a loop (that didn't end at the targetHash) then stop
					}  else if (loopFlag == false){
					    // try extending already-found paths
						waitingList.add(newPath);
						if (CONSOLE_LOG) { System.out.println("searching " + newPath.debugString()); }
					}
					
				}
				
			}
		}
		
		if (CONSOLE_LOG) {
			System.out.println("These are the paths I found:");
			for (int i=0; i < ret.size(); i++) {
				System.out.println(ret.get(i).debugString());
			}
			
			long t1 = System.currentTimeMillis();
			System.out.println("findAllPaths time is: " + (t1-t0) + " msec");
		}
		return ret;	
	}
	/**
	 * returns true/false indicating whether the classCompared is a subclass of the classComparedTo
	 * @param classCompared
	 * @param classComparedTo
	 * @return
	 */
	public Boolean classIsA(OntologyClass classCompared, OntologyClass classComparedTo){
		Boolean retval = false;
		// is classCompared a classComparedTo? recursive check.
		if(classCompared == null || classComparedTo == null) { retval = false; }
		else{
			// get all of the parents of classCompared
			ArrayList<OntologyClass> allParents = this.getClassParents(classCompared);
			allParents.add(classCompared);
			
			for(OntologyClass currentAncestor : allParents){
				// check the current entry, break if we find it. 
				if(currentAncestor.getNameString(false).equalsIgnoreCase(classComparedTo.getNameString(false))){
					retval = true;
					break;
				}
			}			
		}
		return retval;
	}
	
	/**
	 * checks if the classToCheck is in the given range
	 * @param classToCheck
	 * @param range
	 * @return
	 */
	public Boolean classIsInRange(OntologyClass classToCheck, OntologyRange range){
		Boolean retval = false;
		retval = this.classIsA(classToCheck, this.classHash.get(range.getFullName()));
		
		return retval;
	}
	
	/**
	 * loads all of the data for the ontology into the OntologyInfo object
	 * @param endpoint
	 * @param domain
	 * @throws Exception
	 */
	public void load(SparqlEndpointInterface endpoint, String domain) throws Exception {
		// execute each sub-query in order
		endpoint.executeQuery(OntologyInfo.getSuperSubClassQuery(domain), SparqlResultTypes.TABLE);
		this.loadSuperSubClasses(endpoint);
		
		endpoint.executeQuery(OntologyInfo.getTopLevelClassQuery(domain), SparqlResultTypes.TABLE);
		this.loadTopLevelClasses(endpoint);
		
		endpoint.executeQuery(OntologyInfo.getLoadPropertiesQuery(domain), SparqlResultTypes.TABLE);
		this.loadProperties(endpoint);
		
		endpoint.executeQuery(OntologyInfo.getEnumQuery(domain), SparqlResultTypes.TABLE);
		this.loadEnums(endpoint);
		
	}
	
	/**
	 * Build a VisJs representation of OntologyInfo
	 * @return  JSONObject
	 */
	public JSONObject toVisJs() {
		// Questions
		//    Javascript ontologyTree has a namespace node at the root.   Do we want that?
		//    This function gives inherited properties.  Is that ok.
		//    Do we want namespace on the model or not.  This one does.  If we don't, where does it come from?
		//
		JSONObject ret = new JSONObject();
		
		int id = 1;
		HashMap<Integer, String> indexToClass = new HashMap<Integer, String>();
		HashMap<String, Integer> classToIndex = new HashMap<String, Integer>();

		
		// give each class a number
		for (String className : this.classHash.keySet()) {
			classToIndex.put(className, id);
			indexToClass.put(id, className);
			id++;
		}
		
		// create model
		JSONArray model = new JSONArray();
		for (int i=1; i < id; i++) {
			OntologyName classOntName = new OntologyName(indexToClass.get(i));
			JSONObject m = new JSONObject();
			m.put("id", i);
			// use local name of class
			m.put("label", classOntName.getLocalName());
			// use full name of class
			//m.put("label", classOntName.getFullName());
			model.add(m);
		}
		
		// create edges
		JSONArray edges = new JSONArray();
		for (String className : this.subclassHash.keySet()) {
			ArrayList<OntologyClass> subClasses = this.subclassHash.get(className);
			for (int i=0; i < subClasses.size(); i++) {
				JSONObject e = new JSONObject();
				e.put("from", classToIndex.get(className));
				e.put("to", classToIndex.get(subClasses.get(i).getNameString(false)));
				edges.add(e);
			}
		}
		
		// create properties
		JSONObject properties = new JSONObject();
		for (String className : this.classHash.keySet()) {
			
			// create array of properties
			JSONArray propArray = new JSONArray();
			
			// get inherited properties
			ArrayList<OntologyProperty> ontProps = this.getInheritedProperties(this.classHash.get(className)); 
			// get only properties of this class
			//ArrayList<OntologyProperty> ontProps = this.classHash.get(className).getProperties();
			
			for (int i=0; i < ontProps.size(); i++) {
				JSONObject p = new JSONObject();
				OntologyProperty ontProp = ontProps.get(i);
				p.put("type", ontProp.getRangeStr(true));     // true strips name string
				p.put("name",  ontProp.getNameStr(true));      // true strips name string
				propArray.add(p);
			}
			
			properties.put(classToIndex.get(className).toString(), propArray);
		}
		JSONObject data = new JSONObject();
		data.put("model", model);
		data.put("edges", edges);
		data.put("properties", properties);
		ret.put("data", data);
		return ret;
	}
}




















