package com.ge.research.semtk.services.ingestion;

/**
 * For requests that provide a SPARQL connection to override the connection in the template.
 */
public class IngestionFromStringsWithNewConnectionRequestBody extends IngestionFromStringsRequestBody {

	private String connectionOverride;
	
	public String getConnectionOverride(){
		return this.connectionOverride;
	}
	public void setConnectionOverride(String connectionOverride){
		this.connectionOverride = connectionOverride;
	}

}
