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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorCompletionService;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.utility.Utility;
import com.sun.org.apache.bcel.internal.generic.NEW;

/**
 * OntologyInfo is a class that contains the bulk of the understanding of the actual model.
 * it represents all the classes, relationships between them (subclass relations) and related
 * properties. 
 * it provides functionality for determining pathing between two arbitrary classes, enumeration
 * values, and types for properties.
 **/
public class OntologyInfo {

	// --- "permanent" hashes of info about the Ontology ---- //
	// indexed list of all the classes in the ontology of interst
	private HashMap<String, OntologyClass> classHash = new HashMap<String, OntologyClass>();
	// indexed list of all of the properties in the ontoogy of interest
	private HashMap<String, OntologyProperty> propertyHash = new HashMap<String, OntologyProperty>();
	// for each class, list its subclasses. the superclasses are stored in the class object itself. 
	private HashMap<String, ArrayList<OntologyClass>> subclassHash = new HashMap<String, ArrayList<OntologyClass>>();
	// a list of all the enumerations available for a given class. these are handled as full uris for convenience sake. 
	private HashMap<String, ArrayList<String>> enumerationHash = new HashMap<String, ArrayList<String>>();
		
	// --- temporary hashes during path-finding ---
	// for each class, the collection of valid, single-hop paths to and from other classes. 
	private HashMap<String, ArrayList<OntologyPath>> connHash = new HashMap<String, ArrayList<OntologyPath>>();
	private ArrayList<String> pathWarnings = new ArrayList<String>();  // problems incurred searching for a path.	

	private final static int MAXPATHLENGTH = 50;	// how many hops, max, allowed in a returned path between arbitrary nodes
	private static int restCount = 0;               // a list counter
	
	// used in the serialization and have to be held internally in the event that an oInfo is generated 
	// be de-serializing a json blob.
	private SparqlConnection modelConnection;
	
	/**
	 * Default constructor
	 */
	public OntologyInfo(){
	}

	/**
	 * Constructor that also loads oInfo
	 */
	public OntologyInfo(SparqlConnection conn) throws Exception {
		this.loadSparqlConnection(conn);
		this.modelConnection = conn;
	}
	
	/**
	 * Load via the SparqlQueryClient 
	 * @param clientConfig - query client config
	 * @param conn
	 * @throws Exception
	 */
	public OntologyInfo(SparqlQueryClientConfig clientConfig, SparqlConnection conn) throws Exception{
		this.loadSparqlConnection(clientConfig, conn);
		this.modelConnection = conn;
	
	}
	
	public OntologyInfo(JSONObject json) throws Exception {
		this.addJson(json);
	}
	
	public void loadSparqlConnection(SparqlConnection conn) throws Exception {
    	
		ArrayList<SparqlEndpointInterface> modelInterfaces = conn.getModelInterfaces();
		
		for (int i = 0; i < modelInterfaces.size(); i++) {
    		this.load(modelInterfaces.get(i), conn.getDomain());
    	}
    }

	public void loadSparqlConnection(SparqlQueryClientConfig clientConfig, SparqlConnection conn) throws Exception {
    	
		ArrayList<SparqlEndpointInterface> modelInterfaces = conn.getModelInterfaces();
		ArrayList<SparqlQueryClientConfig> configs = clientConfig.getArrayForEndpoints(modelInterfaces);
		
		for (int i = 0; i < configs.size(); i++) {
			
			// check if this is an authorized connection or not. this can be done by looking at the config files.
			SparqlQueryClientConfig curr = configs.get(i);
			
			if(curr instanceof SparqlQueryAuthClientConfig){ 
				this.load(new SparqlQueryClient( (SparqlQueryAuthClientConfig)curr ), conn.getDomain()); 
			}
			else{ 
				this.load(new SparqlQueryClient(curr), conn.getDomain()); 
			}
    	}
    }
	
	
	
	/**
	 * add a new class to the ontology info object. this includes information on super/sub classes
	 * being added to a hash of the known entities.
	 **/
	public void addClass(OntologyClass oClass){
		String classnameStr = oClass.getNameString(false);	// get the full name of the class and do not strip URI info.
		this.connHash.clear(); 
		
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
	
	public ArrayList<String> getSuperclassNames(String subClassName) {
		return this.getSuperclassNames(subClassName, null);
	}
	/**
	 * return a list of the superclasses for a given class.
	 * if there are no known super classes, an empty list is returned.
	 **/
	public ArrayList<String> getSuperclassNames(String subclassName, ArrayList<String> retval){
		// return an arraylist of the superclasses, if any, for a given class
		// the current implementation may return multiple entries for the same value.
		
		if(retval == null){ retval = new ArrayList<String>(); } // if it was null, initialize it.
		
		// add parent name to retval  &  parent class to superclasses
		ArrayList<OntologyClass> superclasses = new ArrayList<OntologyClass>();
		for(String currParentName : this.classHash.get(subclassName).getParentNameStrings(false)){
			retval.add(currParentName);
			superclasses.add(this.classHash.get(currParentName));
		}
		
		// get the Parents' parents.
		for(OntologyClass currParentClass : superclasses){
			// ALWAYS A DUPLICATE, RIGHT? - Paul 5/23/17
			//retval.add(currParentClass.getNameString(false));
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
	public void loadSuperSubClasses(String xList[], String yList[]) throws Exception{

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
	public void loadTopLevelClasses(String xList[]) throws Exception{
		
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
	public void loadEnums(String classList[], String enumValList[] ) throws Exception{
		
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
	
	public static String getAnnotationQuery(String domain) {
		// This query will be sub-optimal if there are multiple labels and comments for many elements
		// because every combination will be returned
		//
		// But in the ususal case where each element has zero or 1 labels and comments
		// It is more efficient to get them in a single query with each element URI only transmitted once.
		String retval = "prefix owl:<http://www.w3.org/2002/07/owl#>\n" + 
				"prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"\n" + 
				"select distinct ?Elem ?Label ?Comment where {\n" + 
				" ?Elem a ?p.\r\n" + 
				" filter regex(str(?Elem),'^" + domain + "'). " + 
				" VALUES ?p {owl:Class owl:DatatypeProperty owl:ObjectProperty}.\n" + 
				"    optional { ?Elem rdfs:label ?Label. }\n" + 
				"    optional { ?Elem rdfs:comment ?Comment. }\n" +
				"}";
		return retval;
	}
	
	public void loadAnnotations(String elemList[], String labelList[], String commentList[]) throws Exception {
		for (int i=0; i < elemList.length; i++) {
			
			// find the element: class or property
			AnnotatableElement e = this.classHash.get(elemList[i]);
			if (e == null) {
				e = this.propertyHash.get(elemList[i]);
			} 
			if (e == null)  {
				throw new Exception("Cannot find element " + elemList[i] + " in the ontology");
			}
			
			// add the annotations (empties and duplicates are handled downstream)
			e.addAnnotationLabel(labelList[i]);
			e.addAnnotationComment(commentList[i]);
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
	public void loadProperties(String classList[], String propertyList[], String rangeList[]) throws Exception{
		 
		// loop through and make the property, pull class...
		for(int i = 0; i < classList.length; i += 1){
			OntologyProperty prop = null;
			
			// does property already exist
			if (this.propertyHash.containsKey(propertyList[i])) {
				prop = this.propertyHash.get(propertyList[i]);
				
				// if property exists, make sure range is the same
				if (! prop.getRangeStr().equals(rangeList[i])) {
					throw new Exception(String.format("SemTk doesn't handle complex ranges.\nClass %s property domain %s\nrange 1: %s\nrange 2: %s",
										  classList[i], 
										  propertyList[i], 
										  this.propertyHash.get(propertyList[i]).getRangeStr(),
										  rangeList[i]));
				}
			} else {
				// if property doesn't exist, create it
				prop = new OntologyProperty(propertyList[i], rangeList[i]);
			}

			OntologyClass c = this.classHash.get(classList[i]);
			if(c == null){
				throw new Exception("Cannot find class " + classList[i] + " in the ontology");
			}			
			c.addProperty(prop);
			
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
		this.loadSuperSubClasses(endpoint.getStringResultsColumn("x"), endpoint.getStringResultsColumn("y"));
		
		endpoint.executeQuery(OntologyInfo.getTopLevelClassQuery(domain), SparqlResultTypes.TABLE);
		this.loadTopLevelClasses(endpoint.getStringResultsColumn("Class"));
		
		endpoint.executeQuery(OntologyInfo.getLoadPropertiesQuery(domain), SparqlResultTypes.TABLE);
		this.loadProperties(endpoint.getStringResultsColumn("Class"),endpoint.getStringResultsColumn("Property"),endpoint.getStringResultsColumn("Range"));
		
		endpoint.executeQuery(OntologyInfo.getEnumQuery(domain), SparqlResultTypes.TABLE);
		this.loadEnums(endpoint.getStringResultsColumn("Class"),endpoint.getStringResultsColumn("EnumVal"));
		
		endpoint.executeQuery(OntologyInfo.getAnnotationQuery(domain), SparqlResultTypes.TABLE);
		this.loadAnnotations(endpoint.getStringResultsColumn("Elem"), endpoint.getStringResultsColumn("Label"), endpoint.getStringResultsColumn("Comment"));
		
	}
	
	/**
	 * loads all of the data for the ontology into the OntologyInfo object
	 * @param endpoint
	 * @param domain
	 * @throws Exception
	 */
	public void load(SparqlQueryClient client, String domain) throws Exception {
		
		// execute each sub-query in order
		TableResultSet tableRes;
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getSuperSubClassQuery(domain), SparqlResultTypes.TABLE);
		this.loadSuperSubClasses(tableRes.getTable().getColumn("x"), tableRes.getTable().getColumn("y"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getTopLevelClassQuery(domain), SparqlResultTypes.TABLE);
		this.loadTopLevelClasses(tableRes.getTable().getColumn("Class"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getLoadPropertiesQuery(domain), SparqlResultTypes.TABLE);
		this.loadProperties(tableRes.getTable().getColumn("Class"), tableRes.getTable().getColumn("Property"), tableRes.getTable().getColumn("Range"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getEnumQuery(domain), SparqlResultTypes.TABLE);
		this.loadEnums(tableRes.getTable().getColumn("Class"), tableRes.getTable().getColumn("EnumVal"));
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toDetailedJSON(SparqlConnection sc) throws Exception {
		
		// build the entire connnection hash
		for(String ocKey : this.classHash.keySet()){
			this.getConnList(ocKey);			
		}

		
		JSONObject retval = new JSONObject();
		JSONObject sparqlConnJSONObject = null;
		
		if(sc != null){
			// use the incoming connection
			sparqlConnJSONObject =sc.toJson();
		}
		
		else if(sc == null && this.modelConnection != null){
			// create a sparql connection to describe where this model came from
			sparqlConnJSONObject = this.modelConnection.toJson();
		}
		else{
			throw new Exception("Error creating JSON Serialization of Ontology Info object. no connection included or embedded.");
		}
		
		
		// get all the classes
		JSONArray classList = this.generateJSONClassArray();
			
		// get all the properties
		JSONArray propertyList = this.generateJSONPropArray();
		
		// get all enumerations
		JSONArray enumList = this.generateJSONEnumerationArray();
		
		// get the date and time of creation.
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		Date logDate = cal.getTime();
		String creationTime = dateFormat.format(logDate);
		
		// add everything.
		// create the oInfo object
		JSONObject ontologyInfoJSONObject = new JSONObject();
		ontologyInfoJSONObject.put("version", 1);
		ontologyInfoJSONObject.put("created", creationTime);
		ontologyInfoJSONObject.put("classList", classList);
		ontologyInfoJSONObject.put("propertyList", propertyList);
		ontologyInfoJSONObject.put("enumerationList", enumList);
		
		retval.put("sparqlConn", sparqlConnJSONObject);
		retval.put("ontologyInfo", ontologyInfoJSONObject);
		
		// ship it out
		return retval;
	}

	@SuppressWarnings("unchecked")
	private JSONArray generateJSONEnumerationArray() {
		JSONArray retval = new JSONArray();

		// get all the enums and what class they descend directly from
		for(String k : this.enumerationHash.keySet()){
			JSONObject currEnum = new JSONObject();
			JSONArray enumList = new JSONArray();
			
			for(String currVal : this.enumerationHash.get(k)){
				enumList.add(currVal);
			}
			
			currEnum.put("fullUri", k);
			currEnum.put("enumeration", enumList);
			retval.add(currEnum);
		}
		
		// ship it out
		return retval;
	}

	@SuppressWarnings("unchecked")
	private JSONArray generateJSONPropArray() {
		JSONArray retval = new JSONArray();

		for(String k : this.propertyHash.keySet()){
			OntologyProperty currProp = this.propertyHash.get(k);
			
			JSONObject propJSONObject = new JSONObject();
			propJSONObject.put("fullUri", currProp.getNameStr(false));
			
			// find the domain. 
			JSONArray domain = new JSONArray();
			
			for(String classUri : this.classHash.keySet()){
				OntologyClass oClass = this.classHash.get(classUri);
				// check to see if this class is in the domain.
				
				ArrayList<OntologyProperty> nowProps = this.getInheritedProperties(oClass);
				if(nowProps  != null){
					for(OntologyProperty p : nowProps){
						if(p.getNameStr(false).equals(currProp.getNameStr(false))){
							domain.add(oClass.getNameString(false));
						}
					}
					
				}
				else{
					// not in the domain.
				}
				
			}
			
			propJSONObject.put("domain", domain);

			// find the range. 
			JSONArray range = new JSONArray();			
			
			range.add( currProp.getRange().getFullName() ); 			// currently, semtk only supports one range item. this might have to change so an array was used.
			
			propJSONObject.put("range", range);
			// add the prop to the return
			retval.add(propJSONObject);
		}
		// ship it out
		return retval;
	}

	@SuppressWarnings("unchecked")
	private JSONArray generateJSONClassArray() {
		// throughout most of our other json generation routines, the objects themselves generate the json for themselves.
		// this instance is different in that oInfo decentralizes a lof the information required for our export.
		// as a result, i am centralizing this to make update/bug fixes less sprawling.
		JSONArray retval = new JSONArray();
		
		// step through the class list and get what we need.
		for(String oCurrUri : this.classHash.keySet()){
			OntologyClass oCurr = this.classHash.get(oCurrUri);
			
			JSONObject currClass = new JSONObject();
			currClass.put("fullUri", oCurrUri); 					// add the full uri for the class

			// get & add the super classes
			JSONArray superClasses = new JSONArray();
			
			for(String parentName : oCurr.getParentNameStrings(false)){
				superClasses.add(parentName);
			}
			currClass.put("superClasses", superClasses);
			
			// get & add the sub classes
			JSONArray subClasses = new JSONArray();
			
			if( this.subclassHash.get(oCurrUri) != null){
				for(OntologyClass sCurr :this.subclassHash.get(oCurrUri) ){
					subClasses.add(sCurr.getNameString(false));
				}
			}
			currClass.put("subClasses", subClasses);
			
			// get & add the properties
			JSONArray propArray = new JSONArray();
			
			ArrayList<OntologyProperty> allProps = this.getInheritedProperties(oCurr);
			
			if(allProps != null){
				for( OntologyProperty oProp : allProps ){
					propArray.add(oProp.getNameStr(false));
				}
			}
			currClass.put("properties", propArray);
			
			// get & add the one-hop connections
			JSONArray oneHop = new JSONArray();
			
			ArrayList<OntologyPath> paths = this.connHash.get(oCurrUri);
			if(paths != null){
				for(OntologyPath oPath : paths){
					JSONObject pCurr = new JSONObject();
					if(oPath.getStartClassName().equals(oCurrUri)) {
						// add the destination.
						pCurr.put("direction", "TO");
						pCurr.put("class", oPath.getEndClassName());
					}
					else{
						// this was the destination
						pCurr.put("direction", "FROM");
						pCurr.put("class", oPath.getStartClassName());
					}
					oneHop.add(pCurr);
				}
			}
			currClass.put("directConnections", oneHop);
	
			// add the current class to the return array
			retval.add(currClass);
			
		}
		
		// ship it out.
		return retval;
	}
	
	/*
     * Build a json that mimics the returns from the load queries
     */
    @SuppressWarnings("unchecked")
	public JSONObject toJson () { 
    	JSONObject json = new JSONObject();
    	
    	JSONArray topLevelClassList =  new JSONArray();
    	JSONArray subClassSuperClassList =  new JSONArray();
    	JSONArray classPropertyRangeList =  new JSONArray();
    	JSONArray classEnumValList =  new JSONArray();
    	JSONArray annotationList = new JSONArray();
    	JSONObject prefixes =  new JSONObject();
        
        HashMap<String,String> prefixToIntHash = new HashMap<String, String>();

        // topLevelClassList and subClassSuperClassList
        for (String c : this.classHash.keySet()) {
            ArrayList<String> parents = this.classHash.get(c).getParentNameStrings(false);   // PEC TODO add support for multiple parents
            String name = this.classHash.get(c).getNameString(false);
            if (parents.size() == 0) {
            	topLevelClassList.add(Utility.prefixURI(name, prefixToIntHash));
            } else {
            	JSONArray a = new JSONArray();
            	a.add(Utility.prefixURI(name, prefixToIntHash));
            	a.add(Utility.prefixURI(parents.get(0), prefixToIntHash));
            	subClassSuperClassList.add(a);
            }
        }
        
        // classPropertyRangeList
        for (String c : this.classHash.keySet()) {
            ArrayList<OntologyProperty> propList = this.classHash.get(c).getProperties();
            for (int i=0; i < propList.size(); i++) {
                OntologyProperty oProp = propList.get(i);
                JSONArray a = new JSONArray();
            	a.add(Utility.prefixURI(c, prefixToIntHash));
            	a.add(Utility.prefixURI(oProp.getNameStr(), prefixToIntHash));
            	a.add(Utility.prefixURI(oProp.getRangeStr(), prefixToIntHash));
                classPropertyRangeList.add(a);
            }
        }
        
        // classEnumValList
        for (String c : this.enumerationHash.keySet()) {
            ArrayList<String> valList = this.enumerationHash.get(c);
            for (int i=0; i < valList.size(); i++) {
                String v = valList.get(i);
                JSONArray a = new JSONArray();
                a.add(Utility.prefixURI(c, prefixToIntHash));
                a.add(Utility.prefixURI(v, prefixToIntHash));
                classEnumValList.add(a);
            }
        }

        // annotationList: classes
        for (String c : this.classHash.keySet()) {
        	ArrayList<String> commentList = this.classHash.get(c).getAnnotationComments();
        	ArrayList<String> labelList = this.classHash.get(c).getAnnotationLabels();
        	int size = Math.max(commentList.size(), labelList.size());
        	for (int i=0; i < size; i++) {
        		JSONArray a = new JSONArray();
                a.add(Utility.prefixURI(c, prefixToIntHash));
                a.add(i >= labelList.size()   ? "" : labelList.get(i));
                a.add(i >= commentList.size() ? "" : commentList.get(i));
                annotationList.add(a);
        	}
        }
        
        // annotationList: properties
        for (String c : this.propertyHash.keySet()) {
        	ArrayList<String> commentList = this.propertyHash.get(c).getAnnotationComments();
        	ArrayList<String> labelList = this.propertyHash.get(c).getAnnotationLabels();
        	int size = Math.max(commentList.size(), labelList.size());
        	for (int i=0; i < size; i++) {
        		JSONArray a = new JSONArray();
                a.add(Utility.prefixURI(c, prefixToIntHash));
                a.add(i >= labelList.size()   ? "" : labelList.get(i));
                a.add(i >= commentList.size() ? "" : commentList.get(i));
                annotationList.add(a);
        	}
        }
        
        
        // prefixes: reverse the hash so its intToPrefix
        for (String p : prefixToIntHash.keySet()) {
            prefixes.put(prefixToIntHash.get(p), p);
        }
        
        json.put("topLevelClassList", topLevelClassList);
    	json.put("subClassSuperClassList", subClassSuperClassList);
    	json.put("classPropertyRangeList",classPropertyRangeList);
    	json.put("classEnumValList", classEnumValList);
    	json.put("annotationList", annotationList);
    	json.put("prefixes", prefixes);
    	
        return json;
    }
    
    public void addJson(JSONObject json) throws Exception {
        
    	// unlike javascript: need to unpack intToPrefixHash
    	HashMap<String,String> intToPrefixHash = new HashMap<String,String>();
    	JSONObject prefixes = (JSONObject) (json.get("prefixes"));
    	
    	for (Object key : prefixes.keySet()) {
    		intToPrefixHash.put((String)key, (String) prefixes.get(key));
    	}
    	
        // unhash topLevelClasses
    	JSONArray jTopLevArr = (JSONArray) json.get("topLevelClassList");
        String [] topLevelClassList = new String[jTopLevArr.size()];
        for (int i=0; i < jTopLevArr.size(); i++) {
            topLevelClassList[i] = Utility.unPrefixURI((String) jTopLevArr.get(i), intToPrefixHash);
        }
        
        this.loadTopLevelClasses(topLevelClassList);
        this.loadSuperSubClasses(Utility.unPrefixJsonTableColumn((JSONArray)json.get("subClassSuperClassList"), 0, intToPrefixHash),
        						 Utility.unPrefixJsonTableColumn((JSONArray)json.get("subClassSuperClassList"), 1, intToPrefixHash)     
                                );
        
        this.loadProperties(Utility.unPrefixJsonTableColumn((JSONArray)json.get("classPropertyRangeList"), 0, intToPrefixHash),
        					Utility.unPrefixJsonTableColumn((JSONArray)json.get("classPropertyRangeList"), 1, intToPrefixHash),
        					Utility.unPrefixJsonTableColumn((JSONArray)json.get("classPropertyRangeList"), 2, intToPrefixHash)
                            );
        
        this.loadEnums(	Utility.unPrefixJsonTableColumn((JSONArray)json.get("classEnumValList"), 0, intToPrefixHash),
        				Utility.unPrefixJsonTableColumn((JSONArray)json.get("classEnumValList"), 1, intToPrefixHash)
                      );
        
        // annotationList is optional for backwards compatibility
        JSONArray annotationList = (JSONArray)json.get("annotationList");
        if (annotationList != null) {
	        this.loadAnnotations(Utility.unPrefixJsonTableColumn(annotationList, 0, intToPrefixHash),
	        					 Utility.getJsonTableColumn(     annotationList, 1),
	        					 Utility.getJsonTableColumn(     annotationList, 2)
	                            );
        }
    }
    
    public String generateRdfOWL(String base) {
    	StringBuilder owl = new StringBuilder();
    	
    	owl.append(String.format(
    			"<rdf:RDF\n" + 
    			"	xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" + 
    			"	xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n" + 
    			"	xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" + 
    			"	xmlns=\"%s#\"\n" + 
    			"	xml:base=\"%s\">\n" + 
    			"	<owl:Ontology rdf:about=\"%s\">\n" + 
    			"		<rdfs:comment xml:lang=\"en\">Created by com.ge.research.semtk.ontologyTools.OntologyInfo</rdfs:comment>\n" + 
    			"	</owl:Ontology>\n",
    			base, base, base)
    		);
    	
    			
    	// Classes
    	owl.append(this.generateXMLComment("****** Classes ******"));

    	for (String c : this.classHash.keySet()) {
    		owl.append(String.format("\t<owl:Class rdf:about=\"%s\">\n", c));
    		
    		// superclasses
    		ArrayList<String> superClasses = this.getSuperclassNames(c);
    		for (int i=0; i < superClasses.size(); i++) {
    			owl.append(String.format("\t\t<rdfs:subClassOf rdf:resource=\"%s\"/>\n", superClasses.get(i)));
    		}
    		
    		// enums
    		if (this.enumerationHash.containsKey(c)) {
    			owl.append(
    					"\t\t<owl:equivalentClass>\n" +
    					"\t\t\t<owl:Class>\n" +
    					"\t\t\t\t<owl:oneOf rdf:parseType=\"Collection\">\n");
    			
    			ArrayList<String> enums = this.enumerationHash.get(c);
    			for (int i=0; i < enums.size(); i++) {
    				owl.append(String.format(
    						"\t\t\t\t\t<rdf:Description rdf:about=\"%s\"/>\n",
    						enums.get(i)));
    			}
    			owl.append(
    					"\t\t\t\t</owl:oneOf>\r\n" + 
    					"\t\t\t</owl:Class>\r\n" + 
    					"\t\t</owl:equivalentClass>\n");
    		}
    		
    		// annotations
    		owl.append(this.classHash.get(c).generateAnnotationRdf("\t\t"));
    		
    		owl.append("\t</owl:Class>\n\n");
    	}
    	
    	// Properties
    	owl.append(this.generateXMLComment("****** Properties ******"));
    	for (String p : this.propertyHash.keySet()) {
    		OntologyProperty oProp = this.propertyHash.get(p);

    		// PEC TODO: this can't be the correct way to determin DatatypeProperty vs ObjectProperty
    		if (oProp.getRangeStr().contains("XMLSchema#")) {
    			owl.append(String.format("\t<owl:DatatypeProperty rdf:about=\"%s\">\n", oProp.getNameStr()));
    		} else {
    			owl.append(String.format("\t<owl:ObjectProperty rdf:about=\"%s\">\n", oProp.getNameStr()));
    		}
    		
    		for (String c : this.classHash.keySet()) {
    			if (this.classHash.get(c).getProperty(p) != null) {
    				owl.append(String.format("\t\t<rdfs:domain rdf:resource=\"%s\"/>\n", c));
    			}
    		}
    		
    		owl.append(String.format("\t\t<rdfs:range rdf:resource=\"%s\"/>\n", oProp.getRangeStr()));
    		
    		// annotations
    		owl.append(oProp.generateAnnotationRdf("\t\t"));
    		
    		if (oProp.getRangeStr().contains("XMLSchema#")) {
    			owl.append("\t</owl:DatatypeProperty>\n\n");
    		} else {
    			owl.append("\t</owl:ObjectProperty>\n\n");
    		}
    	}
    	
    	owl.append("</rdf:RDF>");
    	
    	return owl.toString();
    }
    
    private String generateXMLComment(String comment) {
    	return "\n\n<!-- " + comment + " -->\n\n";
    }
    
    public String generateSADL(String base) {
    	String [] baseTokens = base.split("/");
    	String alias = baseTokens[baseTokens.length - 1];
    	StringBuilder sadl = new StringBuilder();
    	
		sadl.append(String.format("uri \"%s\" alias %s.\n\n", base, alias));
		
		for (String c : this.classHash.keySet()) {
			OntologyClass oClass = this.classHash.get(c);
			ArrayList<OntologyClass> superClasses = this.getClassParents(oClass);
			ArrayList<String> enumVals = this.enumerationHash.get(c);
			ArrayList<OntologyProperty> oProps = oClass.getProperties();
			
			// type
			if (superClasses.size() == 0) {
				sadl.append(String.format("\n%s %s is a class.\n", oClass.getNameString(true), oClass.generateAnnotationsSADL()));
			} else {
				
				for (int i=0; i < superClasses.size(); i++) {
					OntologyClass parent = superClasses.get(i);
					// parent name is full if it isn't in oInfo, abbreviated if it is
					String parentName = parent.getNameString(this.containsClass(parent.getNameString(false)));
					sadl.append(String.format("\n%s %s is a type of %s.\n", oClass.getNameString(true), oClass.generateAnnotationsSADL(), parentName));
				}
			}
			
			// properties
			for (OntologyProperty oProp : oProps) {
				
				String t = oProp.getRangeStr(true);
				sadl.append(String.format("\t%s is described by %s %s with values of type %s.\n", oClass.getNameString(true), oProp.getNameStr(true), oProp.generateAnnotationsSADL(), t));
			}
			
			// enums
			if (enumVals != null) {
				sadl.append(String.format("\t%s must be one of {", oClass.getNameString(true)));
				for (int i=0; i < enumVals.size(); i++) {
					if (i > 0) {
						sadl.append(",");
					}
					OntologyName e = new OntologyName(enumVals.get(i));
					sadl.append(e.getLocalName());
				}
				sadl.append("}.\n");
			}
		}
    	return sadl.toString();
    }
}




















