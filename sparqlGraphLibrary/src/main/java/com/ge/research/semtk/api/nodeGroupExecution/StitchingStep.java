package com.ge.research.semtk.api.nodeGroupExecution;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class StitchingStep {
	String nodegroupId;
	String keyColumns[];
	
	public StitchingStep(String nodegroupId, String[] keyColumns) {
		super();
		this.nodegroupId = nodegroupId;
		this.keyColumns = keyColumns;
	}
	public StitchingStep(JSONObject jObj) throws Exception {
		this.nodegroupId = (String) jObj.get("nodegroupId");
		if (jObj.containsKey("keyColumns")) {
			JSONArray jArr = (JSONArray) jObj.get("keyColumns");
			this.keyColumns = new String[jArr.size()];
			for (int i=0; i < jArr.size(); i++) {
				this.keyColumns[i] = (String) jArr.get(i);
			}
		}
	}
	
	
	public String getNodegroupId() {
		return nodegroupId;
	}
	public void setNodegroupId(String nodegroupId) {
		this.nodegroupId = nodegroupId;
	}
	public String[] getKeyColumns() {
		return keyColumns;
	}
	public void setKeyColumns(String[] keyColumns) {
		this.keyColumns = keyColumns;
	}
	
}
