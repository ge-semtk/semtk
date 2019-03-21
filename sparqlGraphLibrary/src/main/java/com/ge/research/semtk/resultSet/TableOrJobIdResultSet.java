package com.ge.research.semtk.resultSet;

import org.json.simple.JSONObject;

/**
 * ResultSet type that could be either a table or a jobId (that could be used to get a table)
 * 
 * Backwards compatible with TableResultSet in these ways:
 *    sending:  only use the table features if receiver is expecting TableResultSet
 *    receiving:  isTable() will be true if sender sent TableResultSet
 * 
 * @author 200001934
 *
 */
public class TableOrJobIdResultSet extends TableResultSet {
	public final String JOB_ID_KEY = "jobId";
	
	public TableOrJobIdResultSet() {
		super();
	}
	
	public TableOrJobIdResultSet(JSONObject jObj) throws Exception {
		super(jObj);
	}
	
	public void addResults(String jobId) throws Exception {
		if (this.resultsContents != null) {
			throw new Exception("Can't addResults a second time to same TableOrJobIdResultSet");
		}
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(JOB_ID_KEY, jobId); 
		addResultsJSON(jsonObj);
	}
	
	public void addResults(Table table) throws Exception {
		if (this.resultsContents != null) {
			throw new Exception("Can't addResults a second time to same TableOrJobIdResultSet");
		}
		
		super.addResults(table);
	}
	
	public boolean isJobId() {
		return this.resultsContents != null && this.resultsContents.containsKey(JOB_ID_KEY);
	}
	public boolean isTable() {
		return this.resultsContents != null && this.resultsContents.containsKey(TableResultSet.TABLE_JSONKEY);
	}
	
	/**
	 * could be null if ! isJobId()  or if jobId was somehow set to null.
	 * @return
	 */
	public String getJobId() {
		return (String) (this.resultsContents.get(JOB_ID_KEY));
	}

}
