package com.ge.research.semtk.services.ingestion;

public class IngestionFromStringsWithNewConnectionRequestBody {
	private String template;
	private String data;
	private String connectionOverride;
	
	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}
	public String getConnectionOverride(){
		return this.connectionOverride;
	}
	public void setConnectionOverride(String connectionOverride){
		this.connectionOverride = connectionOverride;
	}
	public String getData() {
		try {	
			return data;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	public void setData(String data) {
		this.data = data;
	}
}
