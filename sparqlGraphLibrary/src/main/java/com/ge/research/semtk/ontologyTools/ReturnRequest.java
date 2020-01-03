package com.ge.research.semtk.ontologyTools;


public class ReturnRequest {
	public String domainHintUri;  // optional hint where to look for return URI (e.g. if two classes have property 'name')
	public String returnUri;  // URI to be set to isReturned
	
	public ReturnRequest(String returnUri) {
		super();
		this.domainHintUri = null;
		this.returnUri = returnUri;
	}
	
	public ReturnRequest(String returnUri, String domainHintUri) {
		super();
		this.domainHintUri = domainHintUri;
		this.returnUri = returnUri;
	}
	public String getDomainHintUri() {
		return domainHintUri;
	}
	public void setDomainHintUri(String domainHintUri) {
		this.domainHintUri = domainHintUri;
	}
	public String getReturnUri() {
		return returnUri;
	}
	public void setReturnUri(String returnUri) {
		this.returnUri = returnUri;
	}
	public String toString() {
		return "return: " + returnUri + " domainHint:" + domainHintUri;
	}
}
