package com.ge.research.semtk.edc;

import java.io.File;

import com.ge.research.semtk.auth.ThreadAuthenticator;

public class JobFileInfo {
	
	private String fileId = null;
	private String path = null;
	private String filename = null;
	private String username = ThreadAuthenticator.ANONYMOUS;
	
	/**
	 * Create a file for writing
	 */
	public JobFileInfo() {
	}
	
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getUserName() {
		return username;
	}

	public void setUserName(String username) {
		this.username = username;
	}
	
	public String getFileName() {
		return filename;
	}

	public void setFileName(String filename) {
		this.filename = filename;
	}
	
	public String getFileId() {
		return fileId;
	}


	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

}
