package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Arrays;
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
	private String primaryUri = null;
	private String secondaryUri = null;
	private String secondaryClassUri = null;
	private ArrayList<String> deletePredicatesFromPrimary = null;
	private ArrayList<String> deletePredicatesFromSecondary = null;
	private Hashtable<String,String> primaryLookup = null;
	private Hashtable<String,String> secondaryLookup = null;
	
	/**
	 * Initialize a worker by proposing the primary and secondary Uris, and full list of predicates to delete
	 * @param oInfo
	 * @param conn
	 * @param classUri
	 * @param primaryUri
	 * @param secondaryUri
	 * @param deletePredicatesFromPrimary
	 * @param deletePredicatesFromSecondary
	 * @throws Exception
	 */
	public CombineEntitiesWorker(OntologyInfo oInfo, SparqlConnection conn,
			String primaryUri, String secondaryUri, 
			ArrayList<String> deletePredicatesFromPrimary, ArrayList<String> deletePredicatesFromSecondary) throws Exception {

		this.oInfo = oInfo;
		this.conn = this.buildConn(conn);
		this.primaryUri = primaryUri;
		this.secondaryUri = secondaryUri;
		this.deletePredicatesFromPrimary = deletePredicatesFromPrimary != null ? deletePredicatesFromPrimary : new ArrayList<String>();
		this.deletePredicatesFromSecondary = deletePredicatesFromSecondary != null ? deletePredicatesFromSecondary : new ArrayList<String>();
		
	}
	
	/**
	 * Initialize worker with lookup dicts.
	 * @param oInfo
	 * @param conn
	 * @param primaryLookup
	 * @param secondaryLookup
	 * @param deletePredicatesFromPrimary - delete these from primary, overriding default behavior
	 * @throws Exception
	 */
	public CombineEntitiesWorker(OntologyInfo oInfo, SparqlConnection conn,
			Hashtable<String,String> primaryLookup, Hashtable<String,String> secondaryLookup, 
			ArrayList<String> deletePredicatesFromPrimary,
			ArrayList<String> deletePredicatesFromSecondary) throws Exception {

		this.oInfo = oInfo;
		this.conn = this.buildConn(conn);
		this.primaryLookup = primaryLookup;
		this.secondaryLookup = secondaryLookup;
		this.deletePredicatesFromPrimary = deletePredicatesFromPrimary != null ? deletePredicatesFromPrimary : new ArrayList<String>();
		this.deletePredicatesFromSecondary = deletePredicatesFromSecondary != null ? deletePredicatesFromSecondary : new ArrayList<String>();
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

		this.deleteSkippedProperties();
		this.combineEntities();
		this.deleteSecondary();
	}

	/**
	 * Lookup uri if needed.  Check that uri exists with type(s) and that types are compatible
	 * @throws Exception - any problem
	 */
	public void preCheck() throws Exception {
		
		String primaryTypes = null;
		String secondaryTypes = null;
		
		// lookup primaryUri and type, throwing exception if either is missing
		if (this.primaryUri != null) {
			primaryTypes = this.confirmUriGetType(this.primaryUri, "Primary");
		} else {
			String [] res = this.queryUriAndType(this.primaryLookup, "Primary");
			this.primaryUri = res[0];
			primaryTypes = res[1];
		}
		
		// lookup secondaryUri and type, throwing exception if either is missing
		if (this.secondaryUri != null) {
			secondaryTypes = this.confirmUriGetType(this.secondaryUri, "Secondary");
		} else {
			String [] res = this.queryUriAndType(this.secondaryLookup, "Secondary");
			this.secondaryUri = res[0];
			secondaryTypes = res[1];
		}
		
		// Make sure duplicate's classes are each superclass* of one of target's class
		for (String secondaryTypeName : secondaryTypes.split(" ")) {
			this.secondaryClassUri = secondaryTypeName;
			OntologyClass secondaryClass = this.oInfo.getClass(secondaryTypeName);
			if (secondaryClass == null) {
				throw new Exception(String.format("Secondary uri <%s> is a %s in the triplestore.  Class isn't found in the ontology.", this.secondaryUri, secondaryTypeName));
			}
			boolean okFlag = false;
			for (String primaryTypeName : primaryTypes.split(" ")) {
				OntologyClass primaryClass = this.oInfo.getClass(primaryTypeName);
				if (primaryClass == null) {
					throw new Exception(String.format("Primary uri <%s> is a %s in the triplestore.  Class isn't found in the ontology.", this.primaryUri, primaryTypeName));
				}
				if (this.oInfo.classIsA(primaryClass, secondaryClass)) {
					okFlag = true;
					break;
				}
			}
			if (!okFlag) {
				throw new Exception(String.format("Secondary Uri's class %s is not a superClass* of primary uri's class: %s", secondaryTypeName, primaryTypes));
			}
		}
		
		// find actual primary props and make sure each will be deleted from either primary or secondary
		String primaryPropsQuery =  SparqlToXLibUtil.generateSelectOutgoingProps(this.conn, this.oInfo, primaryUri);
		Table primaryPropsTab = this.conn.getDefaultQueryInterface().executeQueryToTable(primaryPropsQuery);
		for (String primaryPropFound : primaryPropsTab.getColumn(0)) {
			if (! this.deletePredicatesFromPrimary.contains(primaryPropFound)) {
				// delete from secondary unless it is already slated for deletion from primary
				this.deletePredicatesFromSecondary.add(primaryPropFound);
			}
		}
	}
	
	
	/**
	 * Build nodegroup to delete duplicatePredicatesToSkip
	 * @throws Exception
	 */
	public void deleteSkippedProperties() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri

		if (deletePredicatesFromPrimary.size() > 0) {
			String sparql = SparqlToXLibUtil.generateDeleteExactProps(this.conn, this.primaryUri, this.deletePredicatesFromPrimary);
			this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		}
		
		if (this.deletePredicatesFromSecondary.size() > 0) {
			String sparql = SparqlToXLibUtil.generateDeleteExactProps(this.conn, this.secondaryUri, this.deletePredicatesFromSecondary);
			this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		}
	}
	
	/**
	 * Build nodegroup to delete the duplicate
	 * @throws Exception
	 */
	public void combineEntities() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri
		String sparql = SparqlToXLibUtil.generateCombineEntitiesInsertOutgoing(this.conn, this.primaryUri, this.secondaryUri);
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
		
		sparql = SparqlToXLibUtil.generateCombineEntitiesInsertIncoming(this.conn, this.primaryUri, this.secondaryUri);
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(sparql);
	}
	
	/**
	 * Build nodegroup to delete the duplicate
	 * @throws Exception
	 */
	public void deleteSecondary() throws Exception {
		// build one-node nodegroup constrained to this.duplicateUri
		NodeGroup ng = new NodeGroup();
		ng.setSparqlConnection(conn);
		Node n = ng.addNode(this.secondaryClassUri, oInfo);
		n.addValueConstraint(ValueConstraint.buildFilterConstraint(n, "=", this.secondaryUri));
		n.setDeletionMode(NodeDeletionTypes.FULL_DELETE);
		
		// run the query synchronously
		this.conn.getDefaultQueryInterface().executeQueryAndConfirm(ng.generateSparqlDelete());
	}
	
	/**
	 * Lookup uri and type list given lookupHash
	 * @param lookupHash - hash property to val
	 * @param primaryOrSecondary - "primary" or "secondary" for error message
	 * @return String[] - Length 2 is : uri, space-separated-typelist
	 * @throws Exception - problems running query
	 */
	private String[] queryUriAndType(Hashtable<String,String> lookupHash, String primaryOrSecondary) throws Exception {
		String uri = null;
		String query = SparqlToXLibUtil.generateSelectInstance(this.conn, this.oInfo, lookupHash);
		Table tab = this.conn.getDefaultQueryInterface().executeQueryToTable(query);
		if (tab.getNumRows() == 0) {
			throw new Exception("Could not find the " + primaryOrSecondary + " entity");
		} else if (tab.getNumRows() > 1) {
			throw new Exception("Found multiple potential matches for the " + primaryOrSecondary + " entity");
		} else {
			uri = tab.getCell(0, "?uri");
			String typeCell = tab.getCell(0, "?type_list");
			if (typeCell == null || typeCell.isBlank()) 
				throw new Exception(primaryOrSecondary + " uri has no type: " + uri);
		}
		
		return new String [] {uri, tab.getCell(0, "?type_list")};
	}
	
	private String confirmUriGetType(String uri, String primaryOrSecondary) throws Exception {
		String sparql = SparqlToXLibUtil.generateGetInstanceClass(this.conn, this.oInfo, uri);
		Table targetTab = this.conn.getDefaultQueryInterface().executeQueryToTable(sparql);
		
		// If uri and type(s) were not returned, throw the correct error
		if (targetTab.getNumRows() == 0) {
			sparql = SparqlToXLibUtil.generateAskInstanceExists(this.conn, this.oInfo, uri);
			Table tExists = this.conn.getDefaultQueryInterface().executeQueryToTable(sparql);
			if (tExists.getCellAsBoolean(0, 0)) 
				throw new Exception(String.format(primaryOrSecondary + " uri <%s> has no class in the triplestore.", uri));
			else
				throw new Exception(String.format(primaryOrSecondary + " uri <%s> does not exist in the triplestore.", uri));
			
		} 
			
		return String.join(" ", targetTab.getColumn(0));
	}
	
}
