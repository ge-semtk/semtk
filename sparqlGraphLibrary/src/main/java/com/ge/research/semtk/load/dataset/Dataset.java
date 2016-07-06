/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */


package com.ge.research.semtk.load.dataset;

import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

/*
 * A dataset to be imported into a triplestore.
 * Subclass this for specific types of datasets (e.g. CSV, ODBC)
 */
public abstract class Dataset {
	

	/**
	 * Empty constructor
	 */
	public Dataset(){
		// does nothing.
	}
	
	/**
	 * Instantiate a dataset using JSON.
	 * @param config the JSON object
	 * @throws Exception
	 */
	public Dataset(JSONObject config) throws Exception{
		this.fromJSON(config);
	}
	

	/**
	 * Used to instantiate the dataset using JSON.
	 * @param jobj the JSON object
	 * @throws Exception
	 */
	protected abstract void fromJSON(JSONObject jobj) throws Exception;
	
	
	/**
	 * Read the next set of records
	 * @param numRecords the number of records to read
	 * @return the records, as an arraylist of arraylists
	 * @throws Exception
	 */
	public abstract ArrayList<ArrayList<String>> getNextRecords(int numRecords) throws Exception;
	
	
	/**
	 * Get the column names in order
	 * @return an arraylist of column names
	 */
	public abstract ArrayList<String> getColumnNamesinOrder() throws Exception;
	
	/**
	 * Reset a dataset to the first record.
	 */
	public abstract void reset() throws Exception;
	
	/**
	 * Close the dataset
	 */
	public abstract void close() throws Exception;
}
