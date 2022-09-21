package com.ge.research.semtk.ontologyTools;

import java.util.Collection;
import java.util.Hashtable;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;

public class CombineEntitiesInputTable extends Table {
	private Hashtable<String,String> primaryColHash;
	private Hashtable<String,String> secondaryColHash;
	
	/**
	 * Table specifying entities to combine
	 * @param primaryColHash - hash.get(colName) = property URI used to identify primary
	 * @param secondaryColHash - hash.get(colName) = property URI used to identify secondary
	 * @param tab - table containing all the colNames and values
	 * @throws Exception
	 */
	public CombineEntitiesInputTable(Hashtable<String,String> primaryColHash, Hashtable<String,String> secondaryColHash, Table tab) throws Exception {
		super(tab.getColumnNames(), tab.getColumnTypes(), tab.getRows());
		this.primaryColHash = primaryColHash;
		this.secondaryColHash = secondaryColHash;
		
		// allow special property "#type" with any or no prefix 
		for (String k : this.primaryColHash.keySet()) {
			if (this.primaryColHash.get(k).endsWith("#type")) {
				this.primaryColHash.put(k, SparqlToXLibUtil.TYPE_PROP);
				break;
			}
		}
		
		for (String k : this.secondaryColHash.keySet()) {
			if (this.secondaryColHash.get(k).endsWith("#type")) {
				this.secondaryColHash.put(k, SparqlToXLibUtil.TYPE_PROP);
				break;
			}
		}
	}

	public Collection<String> getPrimaryPropNames() {
		return this.primaryColHash.values();
	}
	public Collection<String> getSecondaryPropNames() {
		return this.secondaryColHash.values();
	}
	/**
	 * For all props defining the primary entity, build a hash.get(prop) = value 
	 * @param row
	 * @return
	 * @throws Exception
	 */
	public Hashtable<String,String> getPrimaryPropValHash(int row) throws Exception {
		Hashtable<String, String> ret = new Hashtable<String,String>();
		for (String col : this.primaryColHash.keySet()) {
			String prop = this.primaryColHash.get(col); 
			ret.put(prop, this.getCellAsString(row, col));
		}
		return ret;
	}
	
	/**
	 * For all props defining the secondary entity, build a hash.get(prop) = value 
	 * @param row
	 * @return
	 * @throws Exception
	 */
	public Hashtable<String,String> getSecondaryPropValHash(int row) throws Exception {
		Hashtable<String, String> ret = new Hashtable<String,String>();
		for (String col : this.secondaryColHash.keySet()) {
			String prop = this.secondaryColHash.get(col); 
			ret.put(prop, this.getCellAsString(row, col));
		}
		return ret;
	}
}
