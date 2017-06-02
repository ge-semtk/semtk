package com.ge.research.semtk.services.storedNodegroupExecution;

public class IngestByIdCsvStrRequestBody {

	private String templateId = "";
	private String sparqlConnection = "";
	private String csvContent = "";
	
	public String getTemplateId() {
		return templateId;
	}
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}
	public String getSparqlConnection() {
		return sparqlConnection;
	}
	public void setSparqlConnection(String sparqlConnection) {
		this.sparqlConnection = sparqlConnection;
	}
	public String getCsvContent() {
		return csvContent;
	}
	public void setCsvContent(String csvContent) {
		this.csvContent = csvContent;
	}
}
