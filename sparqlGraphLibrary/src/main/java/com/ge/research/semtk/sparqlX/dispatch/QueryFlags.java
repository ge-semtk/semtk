package com.ge.research.semtk.sparqlX.dispatch;

import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.utility.Utility;

public class QueryFlags {
	HashSet<String> flags = new HashSet<String>();
	
	public QueryFlags() {
	}
	
	public QueryFlags(String jsonStr) throws Exception {
		try {
			if (jsonStr != null) {
				JSONArray jsonArr = Utility.getJsonArrayFromString(jsonStr);
				this.addFlags(jsonArr);
			}
		} catch (Exception e) {
			throw new Exception("Invalid QueryFlags json array string: " + jsonStr, e);
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
	
	public void set (Enum flagVal) {
		this.flags.add(flagVal.name());
	}
	public boolean isSet(Enum flagVal) {
		return this.isSet(flagVal.name());
	}
	
	public boolean isSet(String flag) {
		return this.flags.contains(flag);
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
