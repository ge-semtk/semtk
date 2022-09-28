package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;

public class CombineEntitiesInputTable extends Table {
	private Hashtable<String,String> targetColHash;
	private Hashtable<String,String> duplicateColHash;
	
	/**
	 * Table specifying entities to combine
	 * @param targetColHash - hash.get(colName) = property URI used to identify target
	 * @param duplicateColHash - hash.get(colName) = property URI used to identify duplicate
	 * @param tab - table containing all the colNames and values
	 * @throws Exception
	 */
	public CombineEntitiesInputTable(Hashtable<String,String> targetColHash, Hashtable<String,String> duplicateColHash, Table tab) throws Exception {
		super(tab.getColumnNames(), tab.getColumnTypes(), tab.getRows());
		this.targetColHash = targetColHash;
		this.duplicateColHash = duplicateColHash;
		
		this.replacePropertyAbbrev(this.targetColHash);
		this.replacePropertyAbbrev(this.duplicateColHash);
	}
	
	/**
	 * Replace property abbrev with full property in a Hashtable vals
	 * @param hash
	 */
	private void replacePropertyAbbrev(Hashtable<String,String> hash) {
		for (String k : hash.keySet()) {
			if (hash.get(k).endsWith("#type")) {
				hash.put(k, SparqlToXLibUtil.TYPE_PROP);
			}
		}
	}

	public Collection<String> getTargetPropNames() {
		return this.targetColHash.values();
	}
	public Collection<String> getDuplicatePropNames() {
		return this.duplicateColHash.values();
	}
	/**
	 * For all props defining the target entity, build a hash.get(prop) = value 
	 * @param row
	 * @return
	 * @throws Exception
	 */
	public Hashtable<String,String> getTargetPropValHash(int row) throws Exception {
		Hashtable<String, String> ret = new Hashtable<String,String>();
		for (String col : this.targetColHash.keySet()) {
			String prop = this.targetColHash.get(col); 
			ret.put(prop, this.getCellAsString(row, col));
		}
		return ret;
	}
	
	/**
	 * For all props defining the duplicate entity, build a hash.get(prop) = value 
	 * @param row
	 * @return
	 * @throws Exception
	 */
	public Hashtable<String,String> getDuplicatePropValHash(int row) throws Exception {
		Hashtable<String, String> ret = new Hashtable<String,String>();
		for (String col : this.duplicateColHash.keySet()) {
			String prop = this.duplicateColHash.get(col); 
			ret.put(prop, this.getCellAsString(row, col));
		}
		return ret;
	}
}
