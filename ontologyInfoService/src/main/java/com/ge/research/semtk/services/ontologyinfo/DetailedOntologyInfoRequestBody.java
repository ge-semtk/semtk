package com.ge.research.semtk.services.ontologyinfo;

import com.ge.research.semtk.sparqlX.SparqlConnection;

public class DetailedOntologyInfoRequestBody {

	private String serverType = "";
	private String url = "";
	private String dataset = "";
	private String domain = "";
	
	public String getServerType() {
		return serverType;
	}
	public void setServerType(String serverType) {
		this.serverType = serverType;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getDataset() {
		return dataset;
	}
	public void setDataset(String dataset) {
		this.dataset = dataset;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public SparqlConnection getConnection() throws Exception {
		SparqlConnection retval = new SparqlConnection("ontologyConnection", this.serverType, this.url, "", this.dataset, this.domain);
		return retval;
	}

}
