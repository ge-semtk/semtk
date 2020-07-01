/**
 ** Copyright 2020 General Electric Company
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
package com.ge.research.semtk.load;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.resultSet.Table;

public class DataValidator {
	private HashMap<String,ColumnValidator> validationHash = null;  
	private Table errorTable = null;
	
	/**
	 * 
	 * @param ds
	 * @param colJsonArr JSONArray or null
	 * @throws Exception  - if colJsonArr is not valid
	 */
	public DataValidator(JSONArray colJsonArr) throws Exception {
		this.validationHash = new HashMap<String,ColumnValidator>();  
		
		// Backwards-compatible: handle missing field in json
		if (colJsonArr == null) {
			return;
		}
		
		for (Object o : colJsonArr) {
			ColumnValidator cval = new ColumnValidator((JSONObject) o);
			this.validationHash.put(cval.getColumnName(), cval);
		}
	}
	
	public JSONArray toJsonArray() {
		JSONArray ret = new JSONArray();
		for (ColumnValidator colVal : validationHash.values()) {
			ret.add(colVal.toJson());
		}
		return ret;
	}
	
	/**
	 * loop through dataset and validates. 
	 * @returns int - number of errors
	 * @throws Exception - if trouble reading dataset
	 */
	public int validate(Dataset ds) throws Exception {
		
		// waste no time on no validator
		if (this.validationHash.size() == 0) return 0;
		
		final int BATCH_SIZE = 200;
		int rowNum = 0;
		ArrayList<ArrayList<String>> rows = null;
		int errCount = 0;
		
		ArrayList<String> colNames  = new ArrayList<String>();
		colNames.addAll(ds.getColumnNamesinOrder());
		colNames.add(DataLoader.FAILURE_CAUSE_COLUMN_NAME);
		colNames.add(DataLoader.FAILURE_RECORD_COLUMN_NAME);
		
		// init empty error table
		this.errorTable = new Table(colNames);
		
		HashMap<Integer, ColumnValidator> colToValidatorHash = new HashMap<Integer, ColumnValidator>();
		HashMap<Integer, String> colToNameHash = new HashMap<Integer, String>();
		ArrayList<String> errors = new ArrayList<String>();
		
		// map ds colPos to their validation JsonObjects
		// make sure all MUST_EXIST and NON_EMPTY columns exist
		for (String colName : this.validationHash.keySet()) {
			Integer colPos = ds.getColumnIndex(colName);
			
			if (colPos >= 0) {
				colToValidatorHash.put(colPos, this.validationHash.get(colName));
				colToNameHash.put(colPos, colName);
				
			} else if (this.validationHash.get(colName).mustExist()) {
				errors.add("Missing required column: " + colName);
			} else if (this.validationHash.get(colName).mustExist() || this.validationHash.get(colName).isNonEmpty()) {
				errors.add("Missing column that must be non-empty: " + colName);
			}
		}
		
		// missing columns will come out as errors in "row 0"
		// quit right here.
		if (errors.size() > 0) {
			ArrayList<String> errRow = new ArrayList<String>();
			for (int i=0; i < this.errorTable.getNumColumns() - 2; i++) {
				errRow.add("");
			}
			errRow.add(String.join(";", errors));
			errRow.add(String.valueOf(0));
			this.errorTable.addRow(errRow);
			return errors.size();
		}
		
		ds.reset();
		// validate each row
		while(true){					

			// get more data to clean
			rows = ds.getNextRecords(BATCH_SIZE);
			if(rows.size() == 0){
				break;
			}

			// clean each row
			for(ArrayList<String> row : rows){
				rowNum ++;
				errors = new ArrayList<String>();
				for (Integer colNum : colToValidatorHash.keySet()) {
					errors.addAll( this.validationHash.get(colToNameHash.get(colNum)).validateCell(row.get(colNum)));
				}
				
				if (errors.size() > 0) {
					errCount += errors.size();
					ArrayList<String> errRow = new ArrayList<String>();
					errRow.addAll(row);
					errRow.add(String.join(";", errors));
					errRow.add(String.valueOf(rowNum));
					this.errorTable.addRow(errRow);
				}
			}
		}
		ds.reset();
		return errCount;
	}
	
	public Table getErrorTable() {
		return this.errorTable;
	}

}
