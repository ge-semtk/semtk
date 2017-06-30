package com.ge.research.semtk.services.results;

public class ResultsRequestBodyCsvMaxRows extends ResultsRequestBodyMaxRows {

	public boolean appendDownloadHeaders = false;
	public boolean getAppendDownloadHeaders() {
		return appendDownloadHeaders;
	}

	public void setAppendDownloadHeaders(boolean appendDownloadHeaders) {
		this.appendDownloadHeaders = appendDownloadHeaders;
	}
}
