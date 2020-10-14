/**
 ** Copyright 2018 General Electric Company
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

import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;

/**
 * A dataset created from a Table.
 */
public class TableDataset extends Dataset {

	private Table table;
	private int offset;		// the current offset
	
	/**
	 * Instantiate a dataset.
	 * @param table the table object
	 */
	public TableDataset(Table table) throws Exception{
		this.table = table;
	}
	
	@Override
	protected void fromJSON(JSONObject jobj) throws Exception {
		throw new Exception("Method not implemented yet");
	}

	/**
	 * Read the next set of records
	 * @param numRecords the number of records to read
	 * @return the records, as an arraylist of arraylists
	 * @throws Exception
	 */
	@Override
	public ArrayList<ArrayList<String>> getNextRecords(int numRecords) throws Exception {
		Table retTable = table.slice(offset, numRecords);
		offset += retTable.getNumRows();  // increase the offset by the number of rows retrieved (not the number of rows requested)
		return retTable.getRows();
	}

	/**
	 * Get the column names in order
	 * @return an arraylist of column names
	 */
	@Override
	public ArrayList<String> getColumnNamesinOrder() throws Exception {
		ArrayList<String> headers = new ArrayList<String>(table.getColumnNames().length);
		for(String s: table.getColumnNames()) {
			headers.add(s);
		}
		return headers;
	}

	/**
	 * Reset dataset to the first record.
	 */
	@Override
	public void reset() throws Exception {
		offset = 0;  // reset the offset
	}

	/**
	 * Close the dataset
	 */
	@Override
	public void close() throws Exception {
		// no action needed	
	}

}
