package com.ge.research.semtk.ontologyTools;


public class ReturnProperty {
	public String classUri;
	public String propUri;
	
	public ReturnProperty(String classUri, String propUri) {
		super();
		this.classUri = classUri;
		this.propUri = propUri;
	}
	
	public boolean hasClassUri() {
		return (this.classUri != null && ! this.classUri.isEmpty());
	}
	public boolean hasPropUri() {
		return (this.propUri != null && ! this.propUri.isEmpty());
	}
	public String getClassUri() {
		return this.classUri;
	}
	public String getPropUri() {
		return this.propUri;
	}
}
