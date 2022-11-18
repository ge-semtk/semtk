package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeDeletionTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;

public class CombineEntitiesWorker extends Thread {

	private OntologyInfo oInfo = null;
	private SparqlConnection conn = null;
	private String targetUri = null;
	private String duplicateUri = null;
	private ArrayList<String> deletePredicatesFromTarget = null;
	private ArrayList<String> deletePredicatesFromDuplicate = null;
	private Hashtable<String,String> targetLookup = null;
	private Hashtable<String,String> duplicateLookup = null;
	private RestrictionChecker checker = null;
	private String targetTypeUris[] = null;
	private String duplicateTypeUris[] = null;
	
	/**
	 * Initialize a worker by proposing the target and duplicate Uris, and full list of predicates to delete
	 * @param oInfo
	 * @param conn
	 * @param classUri
	 * @param targetUri
	 * @param duplicateUri
	 * @param deletePredicatesFromTarget
	 * @param deletePredicatesFromDuplicate
	 * @throws Exception 
	 */
	public CombineEntitiesWorker(OntologyInfo oInfo, SparqlConnection conn,
			String targetUri, String duplicateUri, 
			ArrayList<String> deletePredicatesFromTarget, ArrayList<String> deletePredicatesFromDuplicate,
			RestrictionChecker checker) throws Exception {

		this.oInfo = oInfo;
		this.conn = this.buildConn(conn);
		this.targetUri = targetUri;
		this.duplicateUri = duplicateUri;
		this.deletePredicatesFromTarget = deletePredicatesFromTarget != null ? deletePredicatesFromTarget : new ArrayList<String>();
		this.deletePredicatesFromDuplicate = deletePredicatesFromDuplicate != null ? deletePredicatesFromDuplicate : new ArrayList<String>();
		this.checker = checker;
	}
	
	/**
	 * Initialize a worker by proposing the target and duplicate Uris, their types, and full list of predicates to delete
	 * @param oInfo
	 * @param conn
	 * @param classUri
	 * @param targetUri
	 * @param duplicateUri
	 * @param targetTypeNames = non-empty string[] of classes
	 * @param duplicateTypeNames = "
	 * @param duplicateUri* @param deletePredicatesFromTarget
	 * @param deletePredicatesFromDuplicate
	 * @throws Exception 
	 */
	public CombineEntitiesWorker(OntologyInfo oInfo, SparqlConnection conn,
			String targetUri, String duplicateUri, 
			String [] targetTypeNames, String [] duplicateTypeNames,
			ArrayList<String> deletePredicatesFromTarget, ArrayList<String> deletePredicatesFromDuplicate,
			RestrictionChecker checker) throws Exception {

		this.oInfo = oInfo;
		this.conn = this.buildConn(conn);
		this.targetUri = targetUri;
		this.duplicateUri = duplicateUri;
		this.targetTypeUris = targetTypeNames;
		this.duplicateTypeUris = duplicateTypeNames;
		this.deletePredicatesFromTarget = deletePredicatesFromTarget != null ? deletePredicatesFromTarget : new ArrayList<String>();
		this.deletePredicatesFromDuplicate = deletePredicatesFromDuplicate != null ? deletePredicatesFromDuplicate : new ArrayList<String>();
		this.checker = checker;
	}
	
	/**
	 * Initialize worker with lookup dicts.
	 * @param oInfo
	 * @param conn
	 * @param targetLookup
	 * @param duplicateLookup
	 * @param deletePredicatesFromTarget - delete these from target, overriding default behavior
	 * @throws Exception
	 */
	public CombineEntitiesWorker(OntologyInfo oInfo, SparqlConnection conn,
			Hashtable<String,String> targetLookup, Hashtable<String,String> duplicateLookup, 
			ArrayList<String> deletePredicatesFromTarget,
			ArrayList<String> deletePredicatesFromDuplicate,
			RestrictionChecker checker) throws Exception {


		this.oInfo = oInfo;
		this.conn = this.buildConn(conn);
		this.targetLookup = targetLookup;
		this.duplicateLookup = duplicateLookup;
		this.deletePredicatesFromTarget = deletePredicatesFromTarget != null ? deletePredicatesFromTarget : new ArrayList<String>();
		this.deletePredicatesFromDuplicate = deletePredicatesFromDuplicate != null ? deletePredicatesFromDuplicate : new ArrayList<String>();
		this.checker = checker;
		
	}
	
	/**
	 * Replace property abbrev with full property in a Hashtable vals
	 * @param hash
	 */
	public static void replacePropertyAbbrev(ArrayList<String> list) {
		if (list == null) return;
		for (int i=0; i < list.size(); i++) {
			if (list.get(i).endsWith("#type")) {
				list.remove(i);
				list.add(i, SparqlToXLibUtil.TYPE_PROP);
			}
		}
	}
	private SparqlConnection buildConn(SparqlConnection conn) throws Exception {
		SparqlConnection ret = SparqlConnection.deepCopy(conn);
		
		// check only the data connection
		ret.clearModelInterfaces();
		ret.addModelInterface(ret.getDataInterface(0));
		return ret;
	}
	
	/**
	 * Perform the combining, presuming precheck has passed
	 * @throws Exception - any unexpected event in code or triplestore
	 */
	public void combine() throws Exception {

		this.deleteUnwantedProperties();
		this.combineEntities();
		this.deleteDuplicate();
	}

	/**
	 * If they're not already uris, check that target and duplicate exist
	 * Check that target and duplicate types are compatible
	 * 
	 * @throws Exception - unexpected problem
	 * @throws SemtkUserException - problem with inputs intended for user
	 */
	public void preCheck() throws SemtkUserException, Exception {
		
		// lookup targetUri and type, throwing exception if either is missing
		if (this.targetUri != null) {
			if (this.targetTypeUris == null) {
				this.targetTypeUris = this.confirmUriGetType(this.targetUri, "Target").split(" ");
			}
		} else {
			String [] res = this.queryUriAndType(this.targetLookup, "Target");
			this.targetUri = res[0];
			this.targetTypeUris = res[1].split(" ");
		}
		
		// lookup duplicateUri and type, throwing exception if either is missing
		if (this.duplicateUri != null) {
			if (this.duplicateTypeUris == null) {
				this.duplicateTypeUris = this.confirmUriGetType(this.duplicateUri, "Duplicate").split(" ");
			}
		} else {
			String [] res = this.queryUriAndType(this.duplicateLookup, "Duplicate");
			this.duplicateUri = res[0];
			this.duplicateTypeUris = res[1].split(" ");
		}
		
		// Make sure duplicate's classes are each superclass* of one of target's class
		for (String duplicateTypeName : this.duplicateTypeUris) {
			OntologyClass duplicateClass = this.oInfo.getClass(duplicateTypeName);
			if (duplicateClass == null) {
				throw new SemtkUserException(String.format("Duplicate uri <%s> is a %s in the triplestore.  Class isn't found in the ontology.", this.duplicateUri, duplicateTypeName));
			}
			boolean okFlag = false;
			for (String targetTypeName : this.targetTypeUris) {
				OntologyClass targetClass = this.oInfo.getClass(targetTypeName);
				if (targetClass == null) {
					throw new SemtkUserException(String.format("Target uri <%s> is a %s in the triplestore.  Class isn't found in the ontology.", this.targetUri, targetTypeName));
				}
				if (this.oInfo.classIsA(targetClass, duplicateClass)) {
					okFlag = true;
					break;
				}
			}
			if (!okFlag) {
				throw new SemtkUserException(String.format("Duplicate Uri's class %s is not a superClass* of target uri's class: %s", duplicateTypeName, targetTypeUris));
			}
		}
	}
	
	
	/**
	 * Delete deletePredicatesFromTarget, deletePredicatesFromDuplicate, duplicate's type(s), anything that would violate cardinality
	 * @throws Exception
	 */
	public void deleteUnwantedProperties() throws Exception {
		
		// always delete this.deletePredicatesFromTarget properties from the target
		if (deletePredicatesFromTarget.size() > 0) {
			String sparql = SparqlToXLibUtil.generateDeleteExactProps(this.conn, this.targetUri, this.deletePredicatesFromTarget);
			this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		}
		
		// check do cardinality restrictions exist on any duplicate properties:
		Table dupPropCountTab = this.conn.getDefaultQueryInterface().executeQueryToTable(
				SparqlToXLibUtil.generateCountInstanceProperties(conn, oInfo, duplicateUri)
				);
		boolean restrictionsExist = false;
		for (int i=0; i < dupPropCountTab.getNumRows(); i++) {
			if (checker.hasCardinalityRestriction(this.targetTypeUris, dupPropCountTab.getCell(i, "prop"))) {
				restrictionsExist = true;
				break;
			}
		}
		
		// initialize properties to be deleted: 
		// always delete the duplicate's type 
		// (already checked that it/they are superclass* of target)
		HashSet<String> deleteFromDup = new HashSet<String>(this.deletePredicatesFromDuplicate);
		deleteFromDup.add(SparqlToXLibUtil.TYPE_PROP);
		
		// if needed, delete things that would violate cardinality
		if (restrictionsExist) {
			// get target properties' cardinality counts
			Table targetPropCountTab = this.conn.getDefaultQueryInterface().executeQueryToTable(
					SparqlToXLibUtil.generateCountInstanceProperties(conn, oInfo, targetUri)
					);
			
			// loop through all properties not already being delete from duplicate
			for (int i=0; i < dupPropCountTab.getNumRows(); i++) {
				String prop = dupPropCountTab.getCell(i, "prop");
				if (!deleteFromDup.contains(prop)) {
					
					// get total cardinality for duplicate prop after combining
					int targetRow = targetPropCountTab.getFirstMatchingRowNum("prop", prop);
					int total = dupPropCountTab.getCellAsInt(i, "count");
					total += (targetRow >= 0) ? targetPropCountTab.getCellAsInt(targetRow, "count") : 0;
					
					// prepare to delete from duplicate any prop where combining would violate cardinality
					// (regardless of whether it is already violated in target)
					if (!checker.satisfiesCardinality(this.targetTypeUris, prop, total)) {
						deleteFromDup.add(prop);
					}
				}
			}
		}
        
        // delete properties from the duplicate
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(
				SparqlToXLibUtil.generateDeleteExactProps(this.conn, this.duplicateUri, new ArrayList<String>(deleteFromDup))
				);
		
	}
	
	/**
	 * Copy all outgoing and incoming props on duplicate to the target
	 * @throws Exception
	 */
	public void combineEntities() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri
		String sparql = SparqlToXLibUtil.generateCombineEntitiesInsertOutgoing(this.conn, this.targetUri, this.duplicateUri);
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		
		sparql = SparqlToXLibUtil.generateCombineEntitiesInsertIncoming(this.conn, this.targetUri, this.duplicateUri);
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
	}
	
	/**
	 * Delete the duplicate
	 * @throws Exception
	 */
	public void deleteDuplicate() throws Exception {
		String sparql = SparqlToXLibUtil.generateDeleteUri(this.conn, this.duplicateUri);
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
	}
	
	/**
	 * Lookup uri and type list given lookupHash
	 * @param lookupHash - hash property to val
	 * @param targetOrDuplicate - "target" or "duplicate" for error message
	 * @return String[] - Length 2 is : uri, space-separated-typelist
	 * @throws SemtkUserException - problem with data intended for user
	 * @throws Exception - problems running query
	 */
	private String[] queryUriAndType(Hashtable<String,String> lookupHash, String targetOrDuplicate) throws SemtkUserException, Exception {
		String uri = null;
		String query = SparqlToXLibUtil.generateSelectInstance(this.conn, this.oInfo, lookupHash);
		Table tab = this.conn.getDefaultQueryInterface().executeQueryToTable(query);
		if (tab.getNumRows() == 0) {
			throw new SemtkUserException("Could not find the " + targetOrDuplicate + " entity");
		} else if (tab.getNumRows() > 1) {
			throw new SemtkUserException("Found multiple potential matches for the " + targetOrDuplicate + " entity");
		} else {
			uri = tab.getCell(0, "?uri");
			String typeCell = tab.getCell(0, "?type_list");
			if (typeCell == null || typeCell.isBlank()) 
				throw new SemtkUserException(targetOrDuplicate + " uri has no type: " + uri);
		}
		
		return new String [] {uri, tab.getCell(0, "?type_list")};
	}
	
	/**
	 * 
	 * @param uri
	 * @param targetOrDuplicate
	 * @return
	 * @throws SemtkUserException - intended for user
	 * @throws Exception - other problems
	 */
	private String confirmUriGetType(String uri, String targetOrDuplicate) throws SemtkUserException, Exception {
		String sparql = SparqlToXLibUtil.generateGetInstanceClass(this.conn, this.oInfo, uri);
		Table targetTab = this.conn.getDefaultQueryInterface().executeQueryToTable(sparql);
		
		// If uri and type(s) were not returned, throw the correct error
		if (targetTab.getNumRows() == 0) {
			sparql = SparqlToXLibUtil.generateAskInstanceExists(this.conn, this.oInfo, uri);
			Table tExists = this.conn.getDefaultQueryInterface().executeQueryToTable(sparql);
			if (tExists.getCellAsBoolean(0, 0)) 
				throw new SemtkUserException(String.format(targetOrDuplicate + " uri <%s> has no class in the triplestore.", uri));
			else
				throw new SemtkUserException(String.format(targetOrDuplicate + " uri <%s> does not exist in the triplestore.", uri));
			
		} 
			
		return String.join(" ", targetTab.getColumn(0));
	}
	
}
