package com.ge.research.semtk.services.storedNodegroupExecution;

public class DispatchByIdRequestBody {
	private String nodeGroupId;
	private String sparqlConnection;
	private String externalDataConnectionConstraints;
	private String runtimeConstraints;
	
	public void setNodeGroupId(String nodeGroupId){
		this.nodeGroupId = nodeGroupId;
	}
	public String getNodeGroupId(){
		return this.nodeGroupId;
	}
	public String getSparqlConnection() {
		return sparqlConnection;
	}
	public void setSparqlConnection(String sparqlConnection) {
		this.sparqlConnection = sparqlConnection;
	}
	public String getExternalDataConnectionConstraints() {
		return externalDataConnectionConstraints;
	}
	public void setExternalDataConnectionConstraints(String externalDataConnectionConstraints) {
		this.externalDataConnectionConstraints = externalDataConnectionConstraints;
	}
	public String getRuntimeConstraints(){
		return(this.runtimeConstraints);
	}
	public void setRuntimeConstraints(String runtimeConstraints){
		this.runtimeConstraints = runtimeConstraints;
	}
	
}
