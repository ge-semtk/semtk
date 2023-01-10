package com.ge.research.semtk.sparqlX.dispatch;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashSet;

import org.json.simple.JSONArray;

public class QueryFlags {
	// NOTE: query executors and generators and closed-source libraries
	//       may define additional custom flags.  Use a clever scheme to avoid collisions.
	public static final String FLAG_UNOPTIONALIZE_CONSTRAINED = "UNOPTIONALIZE_CONSTRAINED";
	public static final String PRUNE_TO_COL = "PRUNE_TO_COL";
	private static final String DELIM = ":";
	
	HashSet<String> flags = new HashSet<String>();
	
	public QueryFlags() {
	}
	
	public QueryFlags(String flag) throws Exception {
		this.set(flag);
	}
	
	public QueryFlags(String flag, String param) throws Exception {
		this.set(flag + DELIM + param);
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
	
	public void set(String flagVal) {
		this.flags.add(flagVal);
	}
	
	public void set(String flagVal, String param) {
		this.flags.add(flagVal + DELIM + URLEncoder.encode(param, Charset.defaultCharset()));
	}
	
	public boolean isSet(String flagVal) {
		for (String k : this.flags) 
			if (k.equals(flagVal) || k.startsWith(flagVal + DELIM))
				return true;
		return false;
	}
	
	public String get(String flagVal) {
		for (String k : this.flags) 
			if (k.startsWith(flagVal + DELIM))
				return URLDecoder.decode(k.split(DELIM)[1], Charset.defaultCharset());
		return null;
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
