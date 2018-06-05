package com.ge.research.semtk.auth;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * Library-independent representation of http headers
 * @author 200001934
 *
 */
public class HeaderTable {
	Hashtable<String,List<String>> tab = new Hashtable<String,List<String>>();	
	
	public void put(String key, List<String> vals) {
		this.tab.put(key,vals);
	}
	
	public List<String> get(String key) {
		return this.tab.get(key);
	}
	
	public Set<String> keySet() {
		return tab.keySet();
	}
	
}
