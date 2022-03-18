package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ClassInstance {
	public String classUri;
	public String instanceUri;
	
	public ClassInstance(String classUri, String instanceUri) {
		super();
		this.classUri = classUri;
		this.instanceUri = instanceUri;
	} 
	
	public String getClassUri() {
		return classUri;
	}

	public String getInstanceUri() {
		return instanceUri;
	}
	
	public String toString() {
		return "class: " + classUri + " instance:" + instanceUri;
	}
	
	public static ArrayList<String> getClassList(ArrayList<ClassInstance> classInstanceList) {
		ArrayList<String> ret = new ArrayList<String>();
		for (ClassInstance i : classInstanceList) {
			ret.add(i.classUri);
		}
		return ret;
	}

	public static ClassInstance fromJSON(JSONObject o) {
		if (o.containsKey("class")) {
			// backwards compatibility
			return new ClassInstance((String) o.get("class"), (String) o.get("instance"));
		} else {
			return new ClassInstance((String) o.get("classUri"), (String) o.get("instanceUri"));
		}
	}
}
