package com.ge.research.semtk.services.nodegroupStore;

public abstract class StoreDataCsv {
	protected static final String ID = "id";
	protected static final String COMMENTS = "comments";
	protected static final String CREATOR = "creator";
	protected static final String JSON_FILE = "jsonfile";
	
	// for backwards compatibility, this is how headers are written
	protected static final String[] output_headers= {"Context", "ID","comments","creationDate","creator","jsonFile"};
	// these are lower-cased headers actually used
	protected static final String[] required_headers = {ID,COMMENTS, CREATOR, JSON_FILE};

}
