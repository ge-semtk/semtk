package com.ge.research.semtk.sparqlX.dispatch;

import java.util.HashSet;

import org.json.simple.JSONArray;

public class QueryFlags {
	// NOTE: query executors and generators and closed-source libraries
	//       may define additional custom flags.  Use a clever scheme to avoid collisions.
	public static final String FLAG_UNOPTIONALIZE_CONSTRAINED = "UNOPTIONALIZE_CONSTRAINED";
	
	HashSet<String> flags = new HashSet<String>();
	
	public QueryFlags() {
	}
	
	public QueryFlags(String flag) throws Exception {
		this.set(flag);
	}
	
	public QueryFlags(String [] flags) throws Exception {
		for (String f : flags) {
			this.set(f);
		}
	}
	
	public QueryFlags(JSONArray jsonArr) throws Exception {
		try {
			this.addFlags(jsonArr);
		} catch (Exception e) {
			throw new Exception("Invalid QueryFlags json array: " + jsonArr.toJSONString(), e);
		}
	}
	
	private void addFlags(JSONArray jsonArr) {
		if (jsonArr != null) {
			for (int i=0; i < jsonArr.size(); i++) {
				String f = (String) jsonArr.get(i);
				this.flags.add(f);
			}
		}
	}
	
	public boolean isEmpty() {
		return this.flags.isEmpty();
	}
	
	public void set (String flagVal) {
		this.flags.add(flagVal);
	}
	public boolean isSet(String flagVal) {
		return this.flags.contains(flagVal);
	}
	
	public JSONArray toJson() {
		JSONArray ret = new JSONArray();
		for (String f : this.flags) {
			ret.add(f);
		}
		return ret;
	}
	
	public String toJSONString() {
		return this.toJson().toString();
	}
}
