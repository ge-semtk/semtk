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

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

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
	// indexed list of all of the properties in the ontology of interest
	private HashMap<String, OntologyProperty> propertyHash = new HashMap<String, OntologyProperty>();
	// for each class, list its subclasses. the superclasses are stored in the class object itself. 
	private HashMap<String, ArrayList<OntologyClass>> subclassHash = new HashMap<String, ArrayList<OntologyClass>>();
	
	// for each property with subprops, list the sub prop names
	// with just names there are no worries about order of loading or whether props are even used
	private HashMap<String, ArrayList<String>> subpropHash = new HashMap<String, ArrayList<String>>();

	
	// a list of all the enumerations available for a given class. these are handled as full uris for convenience sake. 
	private HashMap<String, ArrayList<String>> enumerationHash = new HashMap<String, ArrayList<String>>();
	
	// --- not stored in json ---
	// pathCountHash(path.toJson().toJSONString()) = count in a some graph somewhere known by caller.
	private HashMap<String, Integer> pathCountHash = new HashMap<String, Integer>();
	
	// --- temporary hashes during path-finding ---
	// for each class, the collection of valid, single-hop paths to and from other classes. 
	private HashMap<String, ArrayList<OntologyPath>> connHash = new HashMap<String, ArrayList<OntologyPath>>();
	private ArrayList<String> pathWarnings = new ArrayList<String>();  // problems incurred searching for a path.	

	private final static int MAXPATHLENGTH = 50;	// how many hops, max, allowed in a returned path between arbitrary nodes
	private static int restCount = 0;               // a list counter
	private final static long JSON_VERSION = 3;
	// used in the serialization and have to be held internally in the event that an oInfo is generated 
	// be de-serializing a json blob.
	private SparqlConnection modelConnection;
	
	private ArrayList<String> loadWarnings = new ArrayList<String>();
	private ArrayList<String> importedGraphs = new ArrayList<String>();
	
	// props found with no Domain.
	private HashMap<String, OntologyProperty> orphanProps = new HashMap<String, OntologyProperty>();
	
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
	 * Deprecated because of performance.  Not clear what the QueryClient adds except overhead.
	 * @param clientConfig - query client config
	 * @param conn
	 * @throws Exception
	 */
	@Deprecated
	public OntologyInfo(SparqlQueryClientConfig clientConfig, SparqlConnection conn) throws Exception{
		this.loadSparqlConnection(clientConfig, conn);
		this.modelConnection = conn;
	
	}
	
	public OntologyInfo(JSONObject json) throws Exception {
		this.addJson(json);
	}
	
	public ArrayList<String> getLoadWarnings() {
		return loadWarnings;
	}

	public ArrayList<String> getImportedGraphs() {
		return importedGraphs;
	}

	/**
	 * Load directly from model sparql endpoint interfaces
	 * @param conn
	 * @throws Exception
	 */
	public void loadSparqlConnection(SparqlConnection conn) throws Exception {
    	
		ArrayList<SparqlEndpointInterface> modelInterfaces = conn.getModelInterfaces();
		
		for (int i = 0; i < modelInterfaces.size(); i++) {
    		this.load(modelInterfaces.get(i), conn.getDomain(), conn.isOwlImportsEnabled());
    	}
    }
	
	public boolean hasSubProperties(OntologyProperty oProp) {
		return this.subpropHash.containsKey(oProp.getNameStr());
	}
	
	public boolean hasSubProperties(String propName) {
		return this.subpropHash.containsKey(propName);
	}

	/**
	 * Load from model sparql endpoint interfaces using a SparqlQueryClient
	 * Deprecated because of performance.  Not clear what the QueryClient adds except overhead.
	 * @param clientConfig
	 * @param conn
	 * @throws Exception
	 */
	@Deprecated
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
				this.load(new SparqlQueryClient(curr), conn.getDomain(), conn.isOwlImportsEnabled()); 
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
	public boolean hasSubclass(String className) {
		return this.subclassHash.get(className) != null;
	}
	
	public ArrayList<String> getSubclassNames(String superClassName) {
		return this.getSubclassNames(superClassName, null);
	}
	/**
	 * return a list of subclass names for a given class.
	 * if there are no known subclasses, an empty list is returned.
	 **/
	private ArrayList<String> getSubclassNames(String superClassName, ArrayList<String> retval){
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
	
	public boolean isSubclassOf(String sub, String maybeSuper) {
		return this.getSubclassNames(maybeSuper).contains(sub);
	}
	
	/**
	 * Get sub properties given valid for all super and sub properties of the domain class URI.
	 * @param superPropertyName
	 * @param domainClassURI
	 * @return
	 */
	public HashSet<String> inferSubPropertyNames(String superPropertyName, String domainClassURI) {
		HashSet<String> ret = new HashSet<String>();
		
		// get list of domainURI class and all it's super classes
		ArrayList<OntologyClass> domainClasses = new ArrayList<OntologyClass>();
		domainClasses.add(this.classHash.get(domainClassURI));
		for (String domainSuperName : this.getSuperclassNames(domainClassURI)) {
			domainClasses.add(this.classHash.get(domainSuperName));
		}
		for (String domainSubName : this.getSubclassNames(domainClassURI)) {
			domainClasses.add(this.classHash.get(domainSubName));
		}
		
		this.addSubPropNames(superPropertyName, domainClasses, ret);
		return ret;
	}
	/**
	 * return a list of subclass names for a given class.
	 * if there are no known subclasses, an empty list is returned.
	 **/
	private void addSubPropNames(String superPropertyName, ArrayList<OntologyClass> domainClasses, HashSet<String> set){
		
		// get list of candidate sub properties
		ArrayList<String> subpropNames = this.subpropHash.get(superPropertyName);
		if (subpropNames != null) {
			for (String subName : subpropNames) {
				
				// if sub-property has domain in domainClasses, add it and it's sub props too
				for (OntologyClass domain : domainClasses) {
					if (domain.getProperty(subName) != null) {
						set.add(subName);
						this.addSubPropNames(subName, domainClasses, set);
						break;
					}
				}
			}
		}
	}
	
	public String findCommonSuperclass(String subClassName1, String subClassName2) {
		ArrayList<String> superClassNames1 = this.getSuperclassNames(subClassName1);
		ArrayList<String> superClassNames2 = this.getSuperclassNames(subClassName2);
		
		superClassNames1.add(0, subClassName1);
		superClassNames2.add(0, subClassName2);
		for (String c : superClassNames1) {
			if (superClassNames2.contains(c)) {
				return c;
			}
		}
		for (String c : superClassNames2) {
			if (superClassNames1.contains(c)) {
				return c;
			}
		}
		return null;
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
		OntologyClass subclass = this.classHash.get(subclassName);
		if (subclass != null) {
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
	
	/**
	 * Get a (giant?) list of every possible one-hop CLASS to CLASS in oInfo
	 * (including subclasses: see getConnList(classNameStr) )
	 * @return
	 * @throws Exception
	 */
	public ArrayList<OntologyPath> getConnList() throws Exception {
		ArrayList<OntologyPath> ret = new ArrayList<OntologyPath>();
		
		for (String classNameStr : this.classHash.keySet()) {
			ret.addAll(this.getConnList(classNameStr));
		}
		return ret;
	}
	
	public boolean isDataProperty(OntologyProperty oProp) {
		return ! this.isObjectProperty(oProp);
	}
	
	public boolean isObjectProperty(OntologyProperty oProp) {
		return this.classHash.keySet().contains(oProp.getRange().getFullName());
	}
	
	public void addPathCount(OntologyPath path, int count) {
		String key = path.toJson().toJSONString();
		this.pathCountHash.put(key, count);
	}
	
	/**
	 * Get all one-hop paths two and from a class
	 *         classFrom -has-> classNameStr
	 *      subClassFrom -has-> classNameStr
	 *                          classNameStr -has->    classTo
	 *                          classNameStr -has-> subClassTo
	 * @param classNameStr
	 * @return
	 * @throws ClassException
	 * @throws PathException
	 */
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
	 * Does oInfo contain any classes starting with this owl file's base
	 * In English: was any version of this owl file loaded into this graph
	 * @param owlStream
	 * @return
	 * @throws Exception
	 */
	public boolean containsClassWithBase(InputStream owlStream) throws Exception {
		String base = Utility.getXmlBaseFromOwlRdf(owlStream);
		return this.containsClassWithBase(base);
	}
	
	/**
	 * Does oInfo contain any classes starting with base.
	 * In English: was any version of this owl file loaded into this graph
	 * @param base
	 * @return
	 */
	public boolean containsClassWithBase(String base) {
		for (String className : this.classHash.keySet()) {
			if (className.startsWith(base + "#")) {
				return true;
			}
		}
		return false;
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
	
	public ArrayList<String> getRangeNames() {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (String k : this.propertyHash.keySet()) {
			OntologyProperty oProp = this.propertyHash.get(k);
			String range = oProp.getRangeStr();
			if (!ret.contains(range)) {
				ret.add(range);
			}
		}
		return ret;
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
	 * Get all pairs of DomainURI, PropURI
	 * @return
	 */
	public ArrayList<String[]> getPropertyPairs() {
		ArrayList<String[]> ret = new ArrayList<String[]>();
		
		for (String cName : this.getClassNames()) {
			OntologyClass oClass = this.getClass(cName);
			for (OntologyProperty oProp : this.getInheritedProperties(oClass)) {
				ret.add(new String[] {cName, oProp.getNameStr()});
			}
		}
		return ret;
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
	public Boolean containsProperty(String propertyUri){
		return this.propertyHash.containsKey(propertyUri);
	}
	
	public OntologyProperty getProperty(String propertyUri) {
		return this.propertyHash.get(propertyUri);
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
		if (!this.classHash.containsKey(oClass.getNameString(false))) {
			return retval;
		}
		
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
	private static String getOwlImportsQuery(String graphName){
		
		
		String retval = "select distinct ?importee from <" + graphName + "> where { " +
						"<" + graphName + "> <http://www.w3.org/2002/07/owl#imports> ?importee. }";
		
		return retval;
	}
	
	/**
	 * Load owl imports if they aren't already loaded
	 * and add loadWarning if the graph is empty
	 * @param sei
	 * @param imports
	 * @throws Exception
	 */
	private void loadOwlImports(SparqlEndpointInterface sei, String [] imports) throws Exception {
		// for each import
		if (imports != null) {
			for (int i=0; i < imports.length; i++) {
				
				if (! this.importedGraphs.contains(imports[i])) {
					// create an sei
					SparqlEndpointInterface importSei = SparqlEndpointInterface.getInstance(
															sei.getServerType(),
															sei.getServerAndPort(),
															imports[i],
															sei.getUserName(),
															sei.getPassword()
															);
					
					// save state
					int numClasses = this.getNumberOfClasses();
					int numEnums = this.getNumberOfEnum();
					int numProperties = this.getNumberOfProperties();
					
					// load
					this.load(importSei, true);
					this.importedGraphs.add(imports[i]);
					
					// check for changes
					if (numClasses == this.getNumberOfClasses() &&
					    numEnums == this.getNumberOfEnum() &&
					    numProperties == this.getNumberOfProperties()) {
						this.loadWarnings.add(imports[i] + " - nothing to import");
					}
				}
			}
		}
	}
	
	/**
	 * Load owl imports if they aren't already loaded
	 * and add loadWarning if the graph is empty
	 * @param client
	 * @param imports
	 * @throws Exception
	 */
	private void loadOwlImports(SparqlQueryClient client, String [] imports) throws Exception {
		if (imports != null) {
			// for each import
			for (int i=0; i < imports.length; i++) {
				
				if (! this.importedGraphs.contains(imports[i])) {
					// create a client
					SparqlQueryAuthClientConfig importClientConfig = new SparqlQueryAuthClientConfig(client.getConfig());
					importClientConfig.setGraph(imports[i]);
					SparqlQueryClient importClient = new SparqlQueryClient(importClientConfig);
					
					// save current state
					int numClasses = this.getNumberOfClasses();
					int numEnums = this.getNumberOfEnum();
					int numProperties = this.getNumberOfProperties();
					
					// load
					this.load(importClient, imports[i]);
					this.importedGraphs.add(imports[i]);
					
					// check for changes
					if (numClasses == this.getNumberOfClasses() &&
					    numEnums == this.getNumberOfEnum() &&
					    numProperties == this.getNumberOfProperties()) {
						this.loadWarnings.add("Import graph has no ontology data or doesn't exist: " + imports[i]);
					}
				}
			}
		}
	}

	/**
	 * returns the sparql for getting the sub and super-class relationships for known classes.
	 **/
	private static String getSuperSubClassQuery(String graphName, String domain){
		// returns a very basic query 
		// domain : something like "caterham.ge.com"
		
		// TODO: inherited logic from the js rendition of this method. fix this method as that one evolves. 
		String retval = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				       	"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				       	"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + 
				       	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				       	"select distinct ?x ?y from <" + graphName + "> where { " +
				       	"?x rdfs:subClassOf ?y " +
				       	getDomainFilterStatement("x", domain) +
				       	getDomainFilterStatement("y", domain) + 
				        " filter (?x != ?y). } order by ?x";
		
		return retval;
	}

	private static String getDomainFilterStatement(String varName, String domain) {
		return getDomainFilterStatement(varName, domain, "");
	}
	
	/**
	 * Generate the domain filter clause.  If none (the new normal) filter out blank nodes.
	 * @param varName
	 * @param domain
	 * @param clause
	 * @return
	 */
	private static String getDomainFilterStatement(String varName, String domain, String clause) {
		if (domain == null || domain.isEmpty()) {
			// remove blank nodes
			String ret = "filter (!regex(str(?" + varName + "),'^nodeID://') " + clause + ") ";
			return ret;
			
		} else {
			// old-fashioned domain filter
			String ret = "filter (regex(str(?" + varName + "),'^" + domain + "') " + clause + ") ";
			return ret;
			
		}
	}
	
	/**
	 * returns the sparql for getting the sub and super-property relationships for known classes.
	 **/
	private static String getSuperSubPropertyQuery(String graphName, String domain){
		
		String retval = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				       	"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				       	"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + 
				       	"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				       	"select distinct ?subProp ?superProp from <" + graphName + "> where { " +
				       	"?subProp rdfs:subPropertyOf ?superProp " +
				       	getDomainFilterStatement("subProp", domain) +
				       	getDomainFilterStatement("superProp", domain) + 
				        " filter (?subProp != ?superProp). } order by ?subProp";
		
		return retval;
	}
	
	/**
	 * process the results of the query to get all of the sub- and super-props query and loads
	 * them into the OntologyInfo object.
	 **/
	public void loadSuperSubProperties(String subPropNames[], String superPropNames[]) throws Exception{
				
		for (int i=0; i < subPropNames.length; i++) {
			// make sure subpropHash entry exists
			if (! this.subpropHash.containsKey(superPropNames[i])) {
				this.subpropHash.put(superPropNames[i], new ArrayList<String>());
			}
			
			// add to subpropHash
			ArrayList<String> subList = this.subpropHash.get(superPropNames[i]);
			if (! subList.contains(subPropNames[i])) {
				subList.add(subPropNames[i]);
			}
			
			// find existing subProp: has domain so loadProperties got it
			OntologyProperty oSubProp = this.propertyHash.get(subPropNames[i]);
			
			// if we found a new orphan: has neither domain nor range so loadProperties didn't find it
			if (oSubProp == null) {
				oSubProp = new OntologyProperty(subPropNames[i], "");
				this.propertyHash.put(subPropNames[i], oSubProp);
				this.orphanProps.put(subPropNames[i], oSubProp);
			}
			
			// If range is "Class", inherit from super property
			// TODO: this is a "bug" or unclear area for SemTK.
			// we are inferring the Range if it isn't specified
			// because otherwise we'll have #Class as a new classURI in oInfo
			// and path-finding issues, etc.
			// We'll come back to this later.
			// Jira:  PESQS-724
			if (oSubProp.getRange().isDefaultClass()) {
				OntologyProperty oSuperProp = this.propertyHash.get(superPropNames[i]);
				oSubProp.setRange(oSuperProp.getRange().deepCopy());
			}
			
		}
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
	private static String getTopLevelClassQuery(String graphName, String domain){
		// domain : something like "caterham.ge.com"
		String retval = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
		       			"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
		       			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + 
		       			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
		       			"select distinct ?Class from <" + graphName + "> { " +
		       			"?Class rdf:type owl:Class " + getDomainFilterStatement("Class", domain) + ". " +
		       			"MINUS " +
		       			"{?Class rdfs:subClassOf ?Sup " +
		       			getDomainFilterStatement("Sup", domain) +
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
	private static String getEnumQuery(String graphName, String domain){
		String retval = "select ?Class ?EnumVal from <" + graphName + "> where { " +
				"  ?Class <http://www.w3.org/2002/07/owl#equivalentClass> ?ec " + getDomainFilterStatement("Class", domain) +". " + 
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
	
	private static String getAnnotationLabelsQuery(String graphName, String domain) {
		// This query will be sub-optimal if there are multiple labels and comments for many elements
		// because every combination will be returned
		//
		// But in the ususal case where each element has zero or 1 labels and comments
		// It is more efficient to get them in a single query with each element URI only transmitted once.
		String retval = "prefix owl:<http://www.w3.org/2002/07/owl#>\n" + 
				"prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"\n" + 
				"select distinct ?Elem ?Label from <" + graphName + "> where {\n" + 
				" ?Elem a ?p " + getDomainFilterStatement("Elem", domain) + ".\r\n" + 
				" VALUES ?p {owl:Class owl:DatatypeProperty owl:ObjectProperty}.\n" + 
				"    optional { ?Elem rdfs:label ?Label. }\n" + 
				"}";
		return retval;
	}
	
	public void loadAnnotationLabels(String elemList[], String labelList[]) throws Exception {
		for (int i=0; i < elemList.length; i++) {
			
			// find the element: class or property
			AnnotatableElement e = this.classHash.get(elemList[i]);
			if (e == null) {
				e = this.propertyHash.get(elemList[i]);
			} 
			// if found (e.g. it isn't a property annotation)
			if (e != null)  {
				// add the annotations (empties and duplicates are handled downstream)
				e.addAnnotationLabel(labelList[i]);
			}
		}
	}
	private static String getAnnotationCommentsQuery(String graphName, String domain) {
		// This query will be sub-optimal if there are multiple labels and comments for many elements
		// because every combination will be returned
		//
		// But in the ususal case where each element has zero or 1 labels and comments
		// It is more efficient to get them in a single query with each element URI only transmitted once.
		String retval = "prefix owl:<http://www.w3.org/2002/07/owl#>\n" + 
				"prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"\n" + 
				"select distinct ?Elem ?Comment from <" + graphName + "> where { \n" + 
				" ?Elem a ?p " + getDomainFilterStatement("Elem", domain) + ". \n" + 
				" VALUES ?p {owl:Class owl:DatatypeProperty owl:ObjectProperty}. \n" + 
				"    optional { ?Elem rdfs:comment ?Comment. }\n" +
				"}";
		return retval;
	}
	
	public void loadAnnotationComments(String elemList[], String commentList[]) throws Exception {
		for (int i=0; i < elemList.length; i++) {
			
			// find the element: class or property
			AnnotatableElement e = this.classHash.get(elemList[i]);
			if (e == null) {
				e = this.propertyHash.get(elemList[i]);
			} 
			// if found (e.g. it isn't a property comment)
			if (e != null)  {
				// add the annotations (empties and duplicates are handled downstream)
				e.addAnnotationComment(commentList[i]);
			}
		}
	}
	
	/**
	 * returns the sparql query used to get anything with a domain or a range.
	 * 
	 * Queries for ?Property rdfs:domain ?Class
	 * 		or the equivalent using lists and/or restrictions.
	 *		IGNORES:   owl:ObjectProperty  and  owl:DatatypeProperty
	 *      USES:      rdfs:domain
	 *
	 * nb:
	 *		- Range or domain may be empty
	 *     
	 * TODO:  extend the OPTIONAL to other clauses in the UNION
	 */
	private static String getLoadPropertiesQuery(String graphName, String domain){
		
		String retval = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
						"PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
						"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
						"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
						"PREFIX  list: <http://jena.hpl.hp.com/ARQ/list#> " +
						"select distinct ?Class ?Property ?Range from <" + graphName + "> where { " + 
						"{" +
							"?Property rdfs:range ?Range " + getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')") + ". \n" +
							"OPTIONAL { ?Property rdfs:domain ?Class " + getDomainFilterStatement("Class", domain) + ". }\n" + 
						"} UNION { \n" +
							"?Property rdfs:domain ?Class " + getDomainFilterStatement("Class", domain) + ". \n" + 
							"OPTIONAL { ?Property rdfs:range ?Range " + getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')") + "} \n" +
						"} UNION { \n" +
							"?Property rdfs:domain ?x. \n" +
							"?x owl:unionOf ?y. \n" +
							buildListMemberSPARQL("?y", "?Class", "filter regex(str(?Class),'^" + domain + "') \n") +
							//"?y list:member ?Class filter regex(str(?Class),'^" + domain + "'). " +
							"?Property rdfs:range ?Range " + getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')") + ". \n" +
						"} UNION { \n" +
							"?Property rdfs:domain ?Class " + getDomainFilterStatement("Class", domain) + ". \n" +
							"?Property rdfs:range ?x.  \n" +
							"?x owl:unionOf ?y. \n" +
					        buildListMemberSPARQL("?y", "?Range", getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')")) + " \n"  + 
						"} UNION { \n" +
							"?Property rdfs:domain ?x. \n" +
							"?x owl:unionOf ?y. \n" +
							buildListMemberSPARQL("?y", "?Class", getDomainFilterStatement("Class", domain)) + " .\n" +
							"?Property rdfs:range ?x1. \n" +
							"?x1 owl:unionOf ?y1. \n" +
					        buildListMemberSPARQL("?y1", "?Range", getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')")) + 
						"} UNION { \n" +
							"?Class rdfs:subClassOf ?x " + getDomainFilterStatement("Class", domain) + ". \n" +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. \n" +
							"?x owl:onClass ?Range " + getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')") + ". \n" +
						"} UNION { \n" +
							"?Class rdfs:subClassOf ?x " + getDomainFilterStatement("Class", domain) + ". \n" +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. ?x owl:onClass ?y. \n" +
							"?y owl:unionOf ?z. \n" +
					        buildListMemberSPARQL("?z", "?Range", getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')")) + " .\n" + 
				        "} UNION { \n" +
							"?x1 owl:unionOf ?x2. \n" + 
					        buildListMemberSPARQL("?x2", "?Class", getDomainFilterStatement("Class", domain) ) + " .\n" +
					        "?x1 rdfs:subClassOf ?x . \n" +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. ?x owl:onClass ?y. \n" +
					        "?y owl:unionOf ?z. \n" +
					        buildListMemberSPARQL("?z", "?Range", getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')")) + " .\n" + 
						"} UNION { \n" +
					        "?Class rdfs:subClassOf ?x " + getDomainFilterStatement("Class", domain) + ". \n" +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. \n" +
					        "?x owl:someValuesFrom ?Range " + getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')") + ". \n" +
						"} UNION { \n" +
					        "?Class rdfs:subClassOf ?x " + getDomainFilterStatement("Class", domain) + ". \n" +
							"?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. \n" +
					        "?x owl:allValuesFrom ?Range " + getDomainFilterStatement("Range", domain, "|| regex(str(?Range),'XML')") + ". \n" +
						"} \n }";
		return retval;
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
	 * 
	 * Properties (data or object) are attached to cl
	 */
	public void loadProperties(String classList[], String propertyList[], String rangeList[]) throws Exception{
		 
		final boolean DEBUG_RANGE = false;   // print message when range is changed to superclass of two different ranges
		
		// loop through and make the property, pull class...
		for(int i = 0; i < classList.length; i += 1){
			OntologyProperty prop = null;
			
			// get prop from propertyHash, or create it
			if (this.propertyHash.containsKey(propertyList[i])) {
				prop = this.propertyHash.get(propertyList[i]);
				
				// if property exists, and range is different 
				// (ignore this if prop RANGE is already defaultClass or new range is empty)
				if (! prop.getRangeStr().equals(rangeList[i]) && !prop.getRange().isDefaultClass() && !rangeList[i].isEmpty()) {
					String superClass = this.findCommonSuperclass(prop.getRangeStr(), rangeList[i]);
					
					if (superClass != null) {
						// Set range to common superclass of different range classes
						// ignore subclass restrictions on ranges for now, until complex ranges are implemented
						// Paul Aug 2020
						
						if (DEBUG_RANGE) {
							LocalLogger.logToStdOut(classList[i] + "->" + propertyList[i] + ": prev: " + prop.getRangeStr() + " curr: " + rangeList[i] + " new super" + superClass);
						}
						
						// make the change
						prop.setRange(new OntologyRange(superClass));
					} else {
						// throw error
						throw new Exception(String.format("SemTk doesn't handle complex ranges.\nClass %s property domain %s\nrange 1: %s\nrange 2: %s",
										  classList[i], 
										  propertyList[i], 
										  this.propertyHash.get(propertyList[i]).getRangeStr(),
										  rangeList[i]));
					}
				}
			} else {
				// if property doesn't exist, create it
				prop = new OntologyProperty(propertyList[i], rangeList[i]);
				this.propertyHash.put(propertyList[i], prop);
			}

			if (classList[i].trim().isEmpty()) {
				// Property has no domain/class. 
				this.orphanProps.put(propertyList[i], prop);
				
			} else {
				// Add property to the class
				OntologyClass c = this.classHash.get(classList[i]);
				if(c == null){
					throw new Exception("Cannot find class " + classList[i] + " in the ontology");
				}			
				c.addProperty(prop);
			}			
		}
		
		// clean up orphans.  Owl may  it once with no domain, and another time with.
		Set<String> toDel = new HashSet<String>();
		for (String orphan : this.orphanProps.keySet()) {
			if (this.getPropertyDomain(this.orphanProps.get(orphan)).size() > 0) {
				toDel.add(orphan);
			}
		}
		
		for (String k : toDel) {
			this.orphanProps.remove(k);
		}
	}
	

	/**
	 * Check validity of the OntologyInfo
	 * @throws Exception
	 */
	public void validate() throws Exception {
		
		// Superclass names must be valid
		for (String className : this.classHash.keySet()) {
			OntologyClass c = this.classHash.get(className);
			for (String superClassName : c.getParentNameStrings(false)) {
				if (! this.classHash.containsKey(superClassName)) {
					throw new Exception("Can't find class" + superClassName + " (superclass of " + className + ") in the ontology");
				}
			}
		}
		
		// check for orphaned properties not resolved by subProp query
		for (String orphan : this.orphanProps.keySet()) {
			this.loadWarnings.add("Property has no domain: " + orphan);
		}
		
		// Note: Range names don't necessarily need to be valid.  As long as they aren't used.
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
	 * Returns enumeration strings for the given class, if it is an enumeration.  Else returns null.
	 * @param classURI
	 * @return
	 */
	public ArrayList<String> getEnumerationStrings(String classURI){
		if(!classIsEnumeration(classURI).booleanValue()){
			return null;
		}
		return this.enumerationHash.get(classURI);
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
	
	public ArrayList<OntologyPath> findAllPaths(String fromClassName, String targetClassName) throws PathException, ClassException {
		ArrayList<String> targetClassNames = new ArrayList<String>();
		targetClassNames.add(targetClassName);
		return this.findAllPaths(fromClassName, targetClassNames, null);
	}
	
	public ArrayList<OntologyPath> findAllPaths(String fromClassName, ArrayList<String> targetClassNames) throws PathException, ClassException {
		return this.findAllPaths(fromClassName, targetClassNames, null);
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
				// PEC 11/2019: there are some good paths that might have "loops" so this will have to be re-thought
				if (waitPath.containsSubPath(conn.get(i))) {
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
						if (CONSOLE_LOG) { LocalLogger.logToStdOut(">>>found path " + newPath.debugString()); }
						
					// PEC CONFUSED: this used to happen every time without any "else" or "else if"
					
					// if path doens't lead to target, add to waiting list
					// But if it is a loop (that didn't end at the targetHash) then stop
					}  else if (loopFlag == false){
					    // try extending already-found paths
						waitingList.add(newPath);
						if (CONSOLE_LOG) { LocalLogger.logToStdOut("searching " + newPath.debugString()); }
					}
					
				}
				
			}
		}
		
		if (CONSOLE_LOG) {
			LocalLogger.logToStdOut("These are the paths I found:");
			for (int i=0; i < ret.size(); i++) {
				LocalLogger.logToStdOut(ret.get(i).debugString());
			}
			
			long t1 = System.currentTimeMillis();
			LocalLogger.logToStdOut("findAllPaths time is: " + (t1-t0) + " msec");
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
		
		if(classCompared == null || classComparedTo == null) { return false; }

		if (classCompared.equals(classComparedTo)) { return true; }
		
		ArrayList<OntologyClass> allParents = this.getClassParents(classCompared);
		
		// recursively classCompared parents
		for (OntologyClass parent : allParents){
			if (this.classIsA(parent, classComparedTo) ) {
				return true;
			}
		}			
		
		return false;
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
	 * Return all OntologyClasses with prop
	 * @param prop
	 * @return
	 */
	public ArrayList<OntologyClass> getPropertyDomain(OntologyProperty prop) {
		ArrayList<OntologyClass> ret = new ArrayList<OntologyClass>();
		
		for (String key : this.classHash.keySet()) {
			OntologyClass oClass = this.classHash.get(key);
			ArrayList<OntologyProperty> oProps = oClass.getProperties();
			if (oProps.contains(prop)) {
				ret.add(oClass);
			}
		}
		return ret;
	}
	
	/**
	 * loads all of the data for the ontology into the OntologyInfo object
	 * @param endpoint
	 * @param domain
	 * @throws Exception
	 */
	@Deprecated
	public void load(SparqlEndpointInterface endpoint, String domain) throws Exception {
		this.load(endpoint, domain, false);
	}
	
	/**
	 * Preferred method of loading an oInfo
	 * @param endpoint
	 * @param owlImportFlag
	 * @throws Exception
	 */
	public void load(SparqlEndpointInterface endpoint, boolean owlImportFlag) throws Exception {
		this.load(endpoint, "", owlImportFlag);
	}
	
	/**
	 * Not-quite-deprecated but not recommended function signature
	 * @param endpoint
	 * @param domain - only remains for backwards compatibility
	 * @param owlImportFlag
	 * @throws Exception
	 */
	public void load(SparqlEndpointInterface endpoint, String domain, boolean owlImportFlag) throws Exception {
		Table tab;
		// find, then recursively load owl imports
		if (owlImportFlag) {
			tab = endpoint.executeQueryToTable(OntologyInfo.getOwlImportsQuery(endpoint.getGraph()));
			this.loadOwlImports(endpoint, tab.getColumn("importee"));
		}
		
		// execute each sub-query in order
		tab = endpoint.executeQueryToTable(OntologyInfo.getSuperSubClassQuery(endpoint.getGraph(), domain));
		this.loadSuperSubClasses(tab.getColumn("x"), tab.getColumn("y"));
		
		tab = endpoint.executeQueryToTable(OntologyInfo.getTopLevelClassQuery(endpoint.getGraph(), domain));
		this.loadTopLevelClasses(tab.getColumn("Class"));
		
		tab = endpoint.executeQueryToTable(OntologyInfo.getLoadPropertiesQuery(endpoint.getGraph(), domain));
		this.loadProperties(tab.getColumn("Class"),tab.getColumn("Property"),tab.getColumn("Range"));
		
		tab = endpoint.executeQueryToTable(OntologyInfo.getSuperSubPropertyQuery(endpoint.getGraph(), domain));
		this.loadSuperSubProperties(tab.getColumn("subProp"), tab.getColumn("superProp"));
		
		tab = endpoint.executeQueryToTable(OntologyInfo.getEnumQuery(endpoint.getGraph(), domain));
		this.loadEnums(tab.getColumn("Class"),tab.getColumn("EnumVal"));
		
		tab = endpoint.executeQueryToTable(OntologyInfo.getAnnotationLabelsQuery(endpoint.getGraph(), domain));
		this.loadAnnotationLabels(tab.getColumn("Elem"), tab.getColumn("Label"));
		
		tab = endpoint.executeQueryToTable(OntologyInfo.getAnnotationCommentsQuery(endpoint.getGraph(), domain));
		this.loadAnnotationComments(tab.getColumn("Elem"), tab.getColumn("Comment"));
		
		this.validate();
	}
	
	/**
	 * loads all of the data for the ontology into the OntologyInfo object
	 * @param threadUnsafeEndpoint
	 * @param domain
	 * @throws Exception
	 */
	public void load(SparqlQueryClient client, String domain) throws Exception {
		this.load(client, domain, false);
	}
		
	public void load(SparqlQueryClient client, String domain, boolean owlImportFlag) throws Exception {
		TableResultSet tableRes;
		String graphName = client.getConfig().getGraph();
		
		if (owlImportFlag) {
			tableRes = (TableResultSet) client.execute(OntologyInfo.getOwlImportsQuery(graphName), SparqlResultTypes.TABLE);
			this.loadOwlImports(client, tableRes.getTable().getColumn("importee"));
		}
		
		// execute each sub-query in order
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getSuperSubClassQuery(graphName, domain), SparqlResultTypes.TABLE);
		this.loadSuperSubClasses(tableRes.getTable().getColumn("x"), tableRes.getTable().getColumn("y"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getTopLevelClassQuery(graphName, domain), SparqlResultTypes.TABLE);
		this.loadTopLevelClasses(tableRes.getTable().getColumn("Class"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getLoadPropertiesQuery(graphName, domain), SparqlResultTypes.TABLE);
		this.loadProperties(tableRes.getTable().getColumn("Class"), tableRes.getTable().getColumn("Property"), tableRes.getTable().getColumn("Range"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getSuperSubPropertyQuery(graphName, domain), SparqlResultTypes.TABLE);
		this.loadSuperSubProperties(tableRes.getTable().getColumn("subProp"), tableRes.getTable().getColumn("superProp"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getEnumQuery(graphName, domain), SparqlResultTypes.TABLE);
		this.loadEnums(tableRes.getTable().getColumn("Class"), tableRes.getTable().getColumn("EnumVal"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getAnnotationLabelsQuery(graphName, domain), SparqlResultTypes.TABLE);
		this.loadAnnotationLabels(tableRes.getTable().getColumn("Elem"), tableRes.getTable().getColumn("Label"));
		
		tableRes = (TableResultSet) client.execute(OntologyInfo.getAnnotationCommentsQuery(graphName, domain), SparqlResultTypes.TABLE);
		this.loadAnnotationComments(tableRes.getTable().getColumn("Elem"), tableRes.getTable().getColumn("Comment"));
		
		this.validate();
	}
		
	/*
     * Build a json that mimics the returns from the load queries
     */
    @SuppressWarnings("unchecked")
	public JSONObject toJson () { 
    	JSONObject json = new JSONObject();
    	
    	JSONArray topLevelClassList =  new JSONArray();
    	JSONArray subClassSuperClassList =  new JSONArray();
    	JSONArray subSuperPropList = new JSONArray();
    	JSONArray classPropertyRangeList =  new JSONArray();
    	JSONArray classEnumValList =  new JSONArray();
    	JSONArray annotationLabelList = new JSONArray();
    	JSONArray annotationCommentList = new JSONArray();
    	JSONObject prefixes =  new JSONObject();
    	JSONArray importedGraphsList = new JSONArray();
    	JSONArray loadWarningsList = new JSONArray();
        
        HashMap<String,String> prefixToIntHash = new HashMap<String, String>();

        // topLevelClassList and subClassSuperClassList
        for (String c : this.classHash.keySet()) {
            ArrayList<String> parents = this.classHash.get(c).getParentNameStrings(false);   // PEC TODO add support for multiple parents
            String name = this.classHash.get(c).getNameString(false);
            if (parents.size() == 0) {
            	topLevelClassList.add(Utility.prefixURI(name, prefixToIntHash));
            } else {
            	for (int i=0; i < parents.size(); i++) {
	            	JSONArray a = new JSONArray();
	            	a.add(Utility.prefixURI(name, prefixToIntHash));
	            	a.add(Utility.prefixURI(parents.get(i), prefixToIntHash));
	            	subClassSuperClassList.add(a);
            	}
            }
        }
        
        for (String superName : this.subpropHash.keySet()) {
        	for (String subName : this.subpropHash.get(superName)) {
        		JSONArray a = new JSONArray();
        		a.add(Utility.prefixURI(subName, prefixToIntHash));
        		a.add(Utility.prefixURI(superName, prefixToIntHash));
        		subSuperPropList.add(a);
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

        // annotation Lists: classes
        for (String c : this.classHash.keySet()) {
        	ArrayList<String> commentList = this.classHash.get(c).getAnnotationComments();
        	for (int i=0; i < commentList.size(); i++) {
        		 JSONArray a = new JSONArray();
                 a.add(Utility.prefixURI(c, prefixToIntHash));
                 a.add(commentList.get(i));
                 annotationCommentList.add(a);
        	}
        	
        	ArrayList<String> labelList = this.classHash.get(c).getAnnotationLabels();
        	for (int i=0; i < labelList.size(); i++) {
        		JSONArray a = new JSONArray();
                a.add(Utility.prefixURI(c, prefixToIntHash));
                a.add(labelList.get(i));
                annotationLabelList.add(a);
        	}
        }
        
        // annotation Lists: properties
        for (String p : this.propertyHash.keySet()) {
        	ArrayList<String> commentList = this.propertyHash.get(p).getAnnotationComments();
        	for (int i=0; i < commentList.size(); i++) {
        		 JSONArray a = new JSONArray();
                 a.add(Utility.prefixURI(p, prefixToIntHash));
                 a.add(commentList.get(i));
                 annotationCommentList.add(a);
        	}
        	
        	ArrayList<String> labelList = this.propertyHash.get(p).getAnnotationLabels();
        	for (int i=0; i < labelList.size(); i++) {
        		JSONArray a = new JSONArray();
                a.add(Utility.prefixURI(p, prefixToIntHash));
                a.add(labelList.get(i));
                annotationLabelList.add(a);
        	}
        }
        
        
        // prefixes: reverse the hash so its intToPrefix
        for (String p : prefixToIntHash.keySet()) {
            prefixes.put(prefixToIntHash.get(p), p);
        }
        
        for (String s : this.importedGraphs) {
        	importedGraphsList.add(s);
        }
        
        for (String s : this.loadWarnings) {
        	loadWarningsList.add(s);
        }
        
        json.put("version", OntologyInfo.JSON_VERSION);
        json.put("topLevelClassList", topLevelClassList);
    	json.put("subClassSuperClassList", subClassSuperClassList);
    	json.put("classPropertyRangeList",classPropertyRangeList);
    	json.put("subSuperPropList", subSuperPropList);
    	json.put("classEnumValList", classEnumValList);
    	json.put("annotationLabelList", annotationLabelList);
    	json.put("annotationCommentList", annotationCommentList);
    	json.put("prefixes", prefixes);
    	json.put("importedGraphsList", importedGraphsList);
    	json.put("loadWarningsList", loadWarningsList);
    	
        return json;
    }
    
    public void addJson(JSONObject json) throws Exception {
        
    	long version = 0;
    	if (json.containsKey("version")) {
    		version = (long)json.get("version");
    	}
    	if (version > OntologyInfo.JSON_VERSION) {
    		throw new Exception(String.format("Can't decode OntologyInfo JSON with newer version > %d: found %d", OntologyInfo.JSON_VERSION, version));
    	}
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
        
        // backward-compatible old version won't have this
        JSONArray superSubPropList = (JSONArray)json.get("subSuperPropList");
        if (superSubPropList != null) {
	        this.loadSuperSubProperties(Utility.unPrefixJsonTableColumn(superSubPropList, 0, intToPrefixHash),
						        		Utility.unPrefixJsonTableColumn(superSubPropList, 1, intToPrefixHash)     
						        		);
        }

        this.loadEnums(	Utility.unPrefixJsonTableColumn((JSONArray)json.get("classEnumValList"), 0, intToPrefixHash),
        				Utility.unPrefixJsonTableColumn((JSONArray)json.get("classEnumValList"), 1, intToPrefixHash)
                      );
        
        // annotationList is optional for backwards compatibility
        JSONArray annotationLabelList = (JSONArray)json.get("annotationLabelList");
        if (annotationLabelList != null) {
	        this.loadAnnotationLabels(	Utility.unPrefixJsonTableColumn(annotationLabelList, 0, intToPrefixHash),
	        					 		Utility.getJsonTableColumn(     annotationLabelList, 1)
	                            	 );
        }
        // optional for backwards compatibility
        JSONArray annotationCommentList = (JSONArray)json.get("annotationCommentList");
        if (annotationCommentList != null) {
	        this.loadAnnotationComments(Utility.unPrefixJsonTableColumn(annotationCommentList, 0, intToPrefixHash),
	        					 		Utility.getJsonTableColumn(     annotationCommentList, 1)
	                            	   );
        }
        // optional for backwards compatibility
        JSONArray importedGraphsList = (JSONArray)json.get("importedGraphsList");
        if (importedGraphsList != null) {
	        for (Object obj : importedGraphsList) {
		        this.importedGraphs.add((String) obj);
	        }
        }
     
        // optional for backwards compatibility
        JSONArray loadWarningsList = (JSONArray)json.get("loadWarningsList");
        if (loadWarningsList != null) {
	        for (Object obj : loadWarningsList) {
		        this.loadWarnings.add((String) obj);
	        }
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
    
    
    // --------------------------------------------------------- Advanced Client Json ---------------------------------------------
    @SuppressWarnings("unchecked")
	public JSONObject toAdvancedClientJson() throws ClassException, PathException{
    	// return the advanced client Json format
    	JSONObject retval = new JSONObject();
    	
    	// create the prefix information.
    	HashMap<String, String> prefixes = new HashMap<String, String>();
    	    	
    	// create the enumeration information
    	JSONArray enumerations = new JSONArray();
    	
    	for(String key : this.enumerationHash.keySet()){
    		String uri = Utility.prefixURI(key, prefixes);
    		
    		JSONObject currEnumeration = new JSONObject();
    		JSONArray values = new JSONArray();				// get all the values.
    		
    		for(String k : this.enumerationHash.get(key)){	// get each of the enumerations we care about
    			String kUri = Utility.prefixURI(k, prefixes);
    			values.add(kUri);
    		}
    		currEnumeration.put("fullUri", uri);			// put the uri in for this enumeration.
    		currEnumeration.put("enumeration", values);
    		
    		enumerations.add(currEnumeration);
    	}
    	
    	// create propertyList information
    	JSONArray propertyList = new JSONArray();
    	
    	for(String key : this.propertyHash.keySet()){
    		String uri = Utility.prefixURI(key, prefixes);
    		
    		JSONObject currProperty = new JSONObject();
    		JSONArray domain = new JSONArray();
    		JSONArray range = new JSONArray();
    		JSONArray labels = new JSONArray();
    		JSONArray comments = new JSONArray();
    		
    		OntologyProperty currProp = this.propertyHash.get(key);
    		
    		// get the domain.
    		for(String oClassKey : this.classHash.keySet()){
    			// get the classes which are in the domain.
    			OntologyClass oClass = this.classHash.get(oClassKey);
    			
    			if(oClass.getProperty(key) != null){
    				// we found one. as a result, this will be prefixed and added.
       				String classId = Utility.prefixURI(oClassKey, prefixes);
    				domain.add(classId);
    			}
    			
    		}
    		
    		// get the range. Okay, this seems silly because we only support one range but it is an array because it 
    		// may change... a lot.
    		String rangeId = Utility.prefixURI(currProp.getRange().getFullName(), prefixes);
    		range.add(rangeId);
    		
    		// add the labels.
    		for(String label : currProp.labels){
    			labels.add(label);
    		}
    		
    		// add the comments
    		for(String comment : currProp.comments){
    			comments.add(comment);
    		}
    		// add all the sub-components to the current property
    		currProperty.put("fullUri", uri);
    		currProperty.put("domain", domain);
    		currProperty.put("range", range);
    		currProperty.put("labels", labels);
    		currProperty.put("comments", comments);    		
    		
    		// add it to the list
    		propertyList.add(currProperty);
    	}
    	
    	// create classList information
    	JSONArray classList = new JSONArray();
    	for(String key : this.classHash.keySet()){
    		String uri = Utility.prefixURI(key, prefixes);
    		
    		JSONObject currClass = new JSONObject();
    		OntologyClass oClass = this.classHash.get(key);
    		JSONArray labels = new JSONArray();
    		JSONArray comments = new JSONArray();
    		JSONArray superClasses = new JSONArray();
    		JSONArray subClasses = new JSONArray();
    		JSONArray directConnections = new JSONArray();
 	
    		// labels
    		for(String label : oClass.labels){
    			labels.add(label);
    		}
    		
    		// comments
    		for(String comment : oClass.comments){
    			comments.add(comment);
    		}
    		// superclasses
    		for(String parent : oClass.getParentNameStrings(false)){
    			String parentPrefixed = Utility.prefixURI(parent, prefixes);
    			superClasses.add(parentPrefixed);
    		}
    		
    		// subclasses
    		ArrayList<OntologyClass> myChildren = this.subclassHash.get(key);
    		if(myChildren != null){
    			for(OntologyClass currChild : myChildren){
    				String childId = Utility.prefixURI(currChild.getNameString(false), prefixes);
    				subClasses.add(childId);
    			}
    		}

    		// directConnections. -- generate the connections. 
    		for(OntologyPath currPath : this.getConnList(key)){
    			JSONObject pathJsonObject = new JSONObject();
    			
    			pathJsonObject.put("startClass", Utility.prefixURI(currPath.getTriple(0).getSubject(), prefixes));
    			pathJsonObject.put("predicate", Utility.prefixURI(currPath.getTriple(0).getPredicate(), prefixes));
    			pathJsonObject.put("destinationClass", Utility.prefixURI(currPath.getTriple(0).getObject(), prefixes));
    			
    			directConnections.add(pathJsonObject);
    		}
    		
    		// full Uri
    		currClass.put("fullUri", uri);
    		currClass.put("superClasses", superClasses);
    		currClass.put("subClasses", subClasses);
    		currClass.put("comments", comments);
    		currClass.put("labels", labels);
    		currClass.put("directConnections", directConnections);
    		
    		classList.add(currClass);
    	}
    	
    	// create the prefix array
    	JSONArray prefixList = new JSONArray();
    	
    	for(String k : prefixes.keySet()){
    		JSONObject pref = new JSONObject();
    		pref.put("prefixId", prefixes.get(k));
    		pref.put("prefix", k);
    		
    		prefixList.add(pref);
    	}
    	
    	String creationTime = ( new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) );
    	
    	retval.put("version", JSON_VERSION);
    	retval.put("generated", creationTime);
    	
    	if(this.modelConnection != null){
    		retval.put("sparqlConn", this.modelConnection.toJson());
    	}
    	
    	JSONObject jsonOInfo = new JSONObject();
    	jsonOInfo.put("prefixes", prefixList);
    	jsonOInfo.put("enumerations", enumerations);
    	jsonOInfo.put("propertyList", propertyList);
    	jsonOInfo.put("classList", classList);
    	
    	retval.put("ontologyInfo", jsonOInfo);
    	
    	// ship it out.
    	return retval;
    }

    // missing subProperty info et al
    @Deprecated
    public void addAdvancedClientJson(JSONObject encodedOInfo) throws Exception{
    	    	
    	// check the version
    	long version = 0;
    	if (encodedOInfo.containsKey("version")) {
    		version = (long)encodedOInfo.get("version");
    	}
    	if (version > OntologyInfo.JSON_VERSION) {
    		throw new Exception(String.format("Can't decode OntologyInfo JSON with newer version > %d: found %d", OntologyInfo.JSON_VERSION, version));
    	}
    	
    	// get the oInfo block:
    	JSONObject oInfoBlock = (JSONObject) encodedOInfo.get("ontologyInfo");
    	
    	// unpack the prefixes.
    	HashMap<String,String> prefixHash = new HashMap<String,String>();
    	JSONArray prefixes = (JSONArray) (oInfoBlock.get("prefixes"));
    	
    	for(int i = 0; i < prefixes.size(); i++){
    		JSONObject currPrefix = (JSONObject) prefixes.get(i);
    		
    		prefixHash.put((String)currPrefix.get("prefixId"), (String)currPrefix.get("prefix"));
    	}
    	
    	// unpack everything in a way that the table processing code (using parallel arrays) can be called.
    	
    	// classes have to be first.
    	JSONArray classes = (JSONArray) (oInfoBlock.get("classList"));
    	
    	// load all the "top-level" classes...
    	ArrayList<String> uris = new ArrayList<String>();
    	
    	for(int a = 0; a < classes.size(); a++){
    		JSONObject currObject = (JSONObject) classes.get(a);
    		JSONArray superClassArr = (JSONArray) currObject.get("superClasses");
    		
    		if(superClassArr == null || superClassArr.size() == 0){
    			String fullUri = (String)currObject.get("fullUri");
    			uris.add(Utility.unPrefixURI(fullUri, prefixHash));
    		}
    	}
    	if(uris.size() != 0){this.loadTopLevelClasses(uris.toArray(new String[uris.size()]));}
    	
    	// load class-subclass info.
    	ArrayList<String> uriSuper = new ArrayList<String>();
     	ArrayList<String> uriSub   = new ArrayList<String>();
    	 
     	for(int a = 0; a < classes.size(); a++){
     		JSONObject currObject = (JSONObject) classes.get(a);
     		JSONArray subClassArr = (JSONArray) currObject.get("subClasses");
     		
     		if(subClassArr != null && subClassArr.size() > 0){
     			String fullUri = (String)currObject.get("fullUri");
     			
     			fullUri = Utility.unPrefixURI(fullUri, prefixHash);
     			
     			for(int b = 0; b < subClassArr.size(); b++){
     				uriSuper.add(fullUri);
     				uriSub.add( Utility.unPrefixURI((String)subClassArr.get(b), prefixHash));
     			}
     			
     		}
     	}
     	if(uriSub.size() != 0){
     		
     		
     		if(uriSub.size() == uriSuper.size()){
     			LocalLogger.logToStdErr("about to load super/sub class relationships... " + uriSub.size() + " units") ;
     			
     			this.loadSuperSubClasses(uriSub.toArray(new String[uriSub.size()]), uriSuper.toArray(new String[uriSuper.size()]));
     		}
     	}
     	     	
    	// load all comments and labels.
     	for(int a = 0; a < classes.size(); a++){
     		JSONObject currObject = (JSONObject) classes.get(a);
     		
     		String fullUri = Utility.unPrefixURI((String)currObject.get("fullUri"), prefixHash);
     		
     		ArrayList<String> commentList = new ArrayList<String>();
         	ArrayList<String> labelList = new ArrayList<String>();
     		ArrayList<String> uri = new ArrayList<String>();
     		
     		if(currObject.containsKey("comments")){
     			JSONArray commentsArr = (JSONArray) currObject.get("comments");
     			
     			for(int h = 0; h < commentsArr.size(); h++){
     				uri.add(fullUri);
     				commentList.add((String)commentsArr.get(h));
     			}
     		}
     		
     		if(uri.size() > 0){ this.loadAnnotationComments(uri.toArray(new String[uri.size()]), commentList.toArray(new String[commentList.size()])); }
     		
     		uri.clear();
     		
     		if(currObject.containsKey("labels")){
         		JSONArray labelsArr = (JSONArray) currObject.get("labels");
     			
         		for(int h = 0; h < labelsArr.size(); h++){
         			uri.add(fullUri);
         			labelList.add((String)labelsArr.get(h));
         		}
     		}
     		
     		if(uri.size() > 0){ this.loadAnnotationLabels(uri.toArray(new String[uri.size()]), labelList.toArray(new String[labelList.size()])); }
     	}
		
    	// unpack the properties second.
    	
    	JSONArray properties = (JSONArray) (oInfoBlock.get("propertyList"));
    	
    	for(int i = 0; i < properties.size(); i++){
    		JSONObject currObject = (JSONObject) properties.get(i);
    		
    		String fullUri = Utility.unPrefixURI((String) currObject.get("fullUri"), prefixHash);
    		JSONArray domain = (JSONArray)currObject.get("domain");
    		JSONArray range = (JSONArray)currObject.get("range");
    		
    		String rangeUri = (String) range.get(0);
    		
    		// create the parallel arrays needed. note that the "domain" is currently the longest possible value for this part.
    		String[] domainVals = new String[domain.size()];
    		String[] uri        = new String[domain.size()];
    		String[] rangeVals	= new String[domain.size()];
    		
    		for(int j = 0; j < domain.size(); j++){
    			uri[j] 			= fullUri;
    			domainVals[j]	= Utility.unPrefixURI((String)domain.get(j), prefixHash);
    			rangeVals[j]	= Utility.unPrefixURI(rangeUri, prefixHash);
    		}
    		// call the property add...
    		this.loadProperties(domainVals, uri, rangeVals);
    		
    		if(currObject.containsKey("labels")){		// checking for backward compat reasons.
	    		JSONArray labels = (JSONArray)currObject.get("labels");
	    		// add the labels for this property
	    		String[] labelVals = new String[labels.size()];
	    		uri = new String[labels.size()];
	    		
	    		for(int k = 0; k < labels.size(); k++){
	    			uri[k] = fullUri;
	    			labelVals[k] = Utility.unPrefixURI((String)labels.get(k), prefixHash);
	    		}
	    		// call the load
	    		this.loadAnnotationLabels(uri, labelVals);
    		}
    		
    		if(currObject.containsKey("comments")){		// also checked for BC reasons.
    			JSONArray comments = (JSONArray)currObject.get("comments");
        		// add the comments for this property
    			String[] commentVals = new String[comments.size()];
    			uri = new String[comments.size()];
    			
    			for(int l = 0; l < comments.size(); l++){
    				uri[l] = fullUri;
    				commentVals[l] = (String)comments.get(l);
    			}
    			// call the load
    			this.loadAnnotationComments(uri, commentVals);
    		}
    	}	
    }

    /**
     * Get a table with all class and property uris, and their labels, if any
     * @return
     * @throws Exception
     */
	public Table getUriLabelTable() throws Exception {
		
		// set up table
		String str = XSDSupportedType.STRING.getFullName();
		Table ret = new Table( new String [] {"type", "uri", "label"},
				               new String [] {str, str, str });
		
		// classes
		for (String key : this.classHash.keySet()) {
			OntologyClass c = this.classHash.get(key);
			ArrayList<String> labels = c.getAnnotationLabels();
			if (labels.size() == 0) {
				ret.addRow(new String [] {"class", key, ""});
			} else {
				for (String l : labels) {
					ret.addRow(new String [] {"class", key, l});
				}
			}
		}
		
		// properties
		for (String key : this.propertyHash.keySet()) {
			OntologyProperty prop = this.propertyHash.get(key);
			ArrayList<String> labels = prop.getAnnotationLabels();
			if (labels.size() == 0) {
				ret.addRow(new String [] {"property", key, ""});
			} else {
				for (String l : labels) {
					ret.addRow(new String [] {"property", key, l});
				}
			}
		}
		return ret;
	}
    
}




















