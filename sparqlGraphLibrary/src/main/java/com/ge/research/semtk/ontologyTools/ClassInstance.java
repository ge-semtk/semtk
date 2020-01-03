package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;

public class ClassInstance {
	public String classUri;
	public String instanceUri;
	
	public ClassInstance(String classUri, String instanceUri) {
		super();
		this.classUri = classUri;
		this.instanceUri = instanceUri;
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
}
