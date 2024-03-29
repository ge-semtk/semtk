package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashSet;

public class PathItemRequest {
	// things that hlep define the classUri
	private String instanceUri = null;      // the uri of the instance
	
	// constraint
	private String labelStr = null;         // a label that uniquely defines the instance
	private String labelPropUri = null;      // property used with the label to uniquely define the instance
	
	private String incomingClassUri = null;  // The instance must be connected to this class
	private String incomingPropUri = null;   // The instance must be connected using this prop
	
	// node that must be added to path
	private String classUri = null;        // 	class of this instance
	
	// parallel arrays
	private ArrayList<String> propUriList = null;
	private ArrayList<String> funcNameList = null;   // TODO mis-named.  NOT aggregate functions.  These are MAX/MIN sort with limit 1
	private ArrayList<String> filterList = null;     // TODO parallel arrays might not be the best idea
	private ArrayList<String> sparqlIDList = null;
	private ArrayList<String> constraintList = null;
	private ArrayList<Boolean> dontReturnList = null;
	
	private boolean returnUri = false;   // default is not to return the classUri.  change for enums, etc.
	
	public PathItemRequest(String classUri) {
		this.classUri = classUri;
	}

	public String getInstanceUri() {
		return instanceUri;
	}

	/**
	 *  
	 * @param instanceUri - value or null
	 */
	public void setInstanceUri(String instanceUri) {
		this.instanceUri = instanceUri;
	}

	public String getIncomingClassUri() {
		return incomingClassUri;
	}

	public void setIncomingClassUri(String incomingClassUri) {
		this.incomingClassUri = incomingClassUri;
	}

	public String getIncomingPropUri() {
		return incomingPropUri;
	}

	public void setIncomingPropUri(String incomingPropUri) {
		this.incomingPropUri = incomingPropUri;
	}

	public String getClassUri() {
		return classUri;
	}

	public void setClassUri(String classUri) {
		this.classUri = classUri;
	}

	public ArrayList<String> getPropUriList() {
		return propUriList;
	}

	public void setPropUriList(ArrayList<String> propUriList) {
		this.propUriList = propUriList;
	}
	
	public String getLabelStr() {
		return labelStr;
	}

	public String getLabelPropUri() {
		return labelPropUri;
	}

	/**
	 * Can be null or an array with size <= to size of propUriList 
	 * with values of null or function name
	 * @return
	 */
	public ArrayList<String> getFuncNameList() {
		return funcNameList;
	}

	public void setFuncNameList(ArrayList<String> funcNameList) {
		this.funcNameList = funcNameList;
	}
	
	public void setFuncName(int pos, String val) throws Exception {
		if (this.funcNameList == null) {
			this.funcNameList = new ArrayList<String>();
		}
		while (funcNameList.size() <= pos ) {
			this.funcNameList.add(null);
		}
		switch (val) {
		case "MIN":
		case "MAX":
			this.funcNameList.set(pos, val);
			break;
		default:
			throw new Exception("Unknown function name: " + val);	
		}
	}
	
	public String getFuncName(String propUri) {
		int i = this.propUriList.indexOf(propUri);
		if (this.funcNameList != null && this.funcNameList.size() > i ) {
			return this.funcNameList.get(i);
		} else {
			return null;
		}
	}

	public void setFilterList(ArrayList<String> funcNameList) {
		this.filterList = funcNameList;
	}
	
	public String getFilter(String propUri) {
		int i = this.propUriList.indexOf(propUri);
		if (this.filterList != null && this.filterList.size() > i ) {
			return this.filterList.get(i);
		} else {
			return null;
		}
	}
	
	public ArrayList<String> getConstraintList() {
		return constraintList;
	}

	public void setConstraintList(ArrayList<String> constraintList) {
		this.constraintList = constraintList;
	}

	public ArrayList<Boolean> getDontReturnList() {
		return dontReturnList;
	}

	public void setDontReturnList(ArrayList<Boolean> dontReturnList) {
		this.dontReturnList = dontReturnList;
	}
	
	public boolean getReturnUri() {
		return returnUri;
	}

	public void setReturnUri(boolean returnUri) {
		this.returnUri = returnUri;
	}

	public void addPropUri(String val) {
		if (this.propUriList == null) this.propUriList = new ArrayList<String>();
		this.propUriList.add(val);
	}
	
	public HashSet<String> getClassList() {
		HashSet<String> ret = new HashSet<String>();
		
		ret.add(this.classUri);
		if (this.incomingClassUri != null) 
			ret.add(this.incomingClassUri);
		return ret;
	}
	
	public void setSparqlID(int pos, String val) throws Exception {
		if (this.sparqlIDList == null) {
			this.sparqlIDList = new ArrayList<String>();
		}
		while (sparqlIDList.size() <= pos ) {
			this.sparqlIDList.add(null);
		}
		
		this.sparqlIDList.set(pos, val);
	}
	
	public String getSparqlID(String propUri) {
		int i = this.propUriList.indexOf(propUri);
		if (this.sparqlIDList != null && this.sparqlIDList.size() > i ) {
			return this.sparqlIDList.get(i);
		} else {
			return null;
		}
	}
	
	public Triple getTripleHint() {
		if (this.incomingClassUri != null) 
			return new Triple(this.incomingClassUri, this.incomingPropUri, this.classUri);
		else
			return null;
	}
	
	/**
	 * Get all the classes in a list of PathItemRequests
	 * @param requestList
	 * @return
	 */
	public static HashSet<String> getClassesInList(ArrayList<PathItemRequest> requestList) {
		HashSet<String> ret = new HashSet<String>();
		
		for (PathItemRequest req : requestList) {
			ret.add(req.getClassUri());
			if (req.getIncomingClassUri() != null) {
				ret.add(req.getIncomingClassUri());
			}
		}
		return ret;
	}
	
	public static ArrayList<Triple> getTripleHintsInList(ArrayList<PathItemRequest> requestList) {
		ArrayList<Triple> ret = new ArrayList<Triple>();
		
		for (PathItemRequest req : requestList) {
			if (req.getTripleHint() != null) {
				ret.add(req.getTripleHint());
			}
		}
		return ret;
	}
	
	public static PathItemRequest getFirstInList(ArrayList<PathItemRequest> requestList, String classUri) {
		
		for (PathItemRequest req : requestList) {
			if (req.getClassUri().equals(classUri)) {
				return req;
			}
		}
		return null;
	}
	
	public static int countPropUriInList(ArrayList<PathItemRequest> requestList, String propUri) {
		int ret = 0;
		for (PathItemRequest req : requestList) {
			if (req.getPropUriList() != null && req.getPropUriList().contains(propUri)) {
				ret++;
			}
		}
		return ret;
	}
	/**
	 * Return path or null if no incoming anything
	 * @return
	 * @throws PathException
	 */
	public OntologyPath getIncomingPath() throws PathException {
		if (this.getIncomingClassUri() == null || this.getIncomingPropUri() == null) {
			return null;
		} else {
			OntologyPath ret = new OntologyPath(this.getIncomingClassUri());
			ret.addTriple(this.getIncomingClassUri(), this.getIncomingPropUri(), this.getClassUri());
			return ret;
		}
	}

	public void setLabelStr(String labelStr) {
		this.labelStr = labelStr;
	}
	public void setLabelPropUri(String propertyUri) {
		this.labelPropUri = propertyUri;
	}
}
