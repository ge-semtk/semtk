package com.ge.research.semtk.springutilib.requests;

public class StoredItemRenameRequest extends StoredItemRequest {

	private String newId = "";

	public String getNewId() throws Exception {
		return newId;
	}

	public void setNewId(String newId) {
		this.newId = newId;
	}

}
