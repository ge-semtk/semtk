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
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.dataset.Dataset;

public class DataValidator {
	private Dataset ds = null;
	private HashMap<Integer,JSONObject> colHash = null;  // colHash<colIndex> = colValidationJson
	private ArrayList<String> colNames = null;;
	private StringBuffer errors = new StringBuffer();
	private int errorCount = 0;
	private String [] regexMatches = null;     // String regex for column index, or null
	private String [] regexNoMatch = null;        // String regex for column index, or null
	private Pattern [] patternMatches = null;     // Pattern for column index, or null
	private Pattern [] patternNoMatch = null;        // Pattern for column index, or null
	
	private final String KEY_COL_NAME = "colName";
	
	// supported validations
	private final String KEY_NOT_EMPTY = "notEmpty";
	private final String KEY_REGEX_MATCHES = "regexMatches";
	private final String KEY_REGEX_NOMATCH = "regexNoMatch";
	
	private final String KEY_TYPE = "type";
	private final String VAL_TYPE_INT = "int";
	private final String VAL_TYPE_FLOAT = "float";
	
	private final String KEY_ARITH_LT = "lt";
	private final String KEY_ARITH_GT = "gt";
	private final String KEY_ARITH_LTE = "lte";
	private final String KEY_ARITH_GTE = "gte";
	private final String KEY_ARITH_NE = "ne";
	
	/**
	 * 
	 * @param ds
	 * @param colJsonArr
	 * @throws Exception  - if colJsonArr is not valid
	 */
	public DataValidator(Dataset ds, JSONArray colJsonArr) throws Exception {
		this.ds = ds;
		
		// save col names
		this.colNames = ds.getColumnNamesinOrder();
		
		this.regexMatches = new String[colNames.size()];
		this.regexNoMatch = new String[colNames.size()];
		this.patternMatches = new Pattern[colNames.size()];
		this.patternNoMatch = new Pattern[colNames.size()];
		this.colHash = new HashMap<Integer, JSONObject>();
				
		// build colHash, regexMatches, regexNoMatch
		for (Object o : colJsonArr) {
			JSONObject colJson = (JSONObject) o;
			String colName = (String)colJson.get(KEY_COL_NAME);
			// silently ignore colJson that doesn't have a "colName"
			if (colName != null) {
				int colIndex = -1;
				try {
					colIndex = ds.getColumnIndex(colName);
					colHash.put(colIndex, colJson);
				} catch (Exception e) {
					throw new Exception("Input data is missing a validated column: " + colName);
				}
				
				if (colJson.containsKey(KEY_REGEX_MATCHES)) {
					this.regexMatches[colIndex] = (String) colJson.get(KEY_REGEX_MATCHES);
					this.patternMatches[colIndex] = Pattern.compile(regexMatches[colIndex]);

				}
				if (colJson.containsKey(KEY_REGEX_NOMATCH)) {
					this.regexNoMatch[colIndex] = (String) colJson.get(KEY_REGEX_NOMATCH);
					this.patternNoMatch[colIndex] = Pattern.compile(regexNoMatch[colIndex]);

				}
			}
		}
		
		this.validateTheJson();
	}
	
	/**
	 * loop through dataset and validates. 
	 * @returns int - number of errors
	 * @throws Exception - if trouble reading dataset
	 */
	public int validate() throws Exception {
		final int BATCH_SIZE = 200;
		int rowNum = 0;
		ArrayList<ArrayList<String>> rows = null;
		while(true){					

			// get more data to clean
			rows = this.ds.getNextRecords(BATCH_SIZE);
			if(rows.size() == 0){
				break;  // no more data, done
			}

			// clean each row
			for(ArrayList<String> row : rows){
				rowNum ++;
				this.validateRow(row, rowNum);
			}
		}
		return this.errorCount;
	}
	
	public String getErrorString() {
		return this.errors.toString();
	}
	private void validateRow(ArrayList<String> row, int rowNum) {
		for (int colNum : this.colHash.keySet()) {
			this.validateCell(row.get(colNum), rowNum, colNum);
		}
	}
	
	/**
	 * Record the first error found in a cell via addError()
	 * @param val
	 * @param row
	 * @param col
	 */
	private void validateCell(String val, int row, int col) {
		JSONObject validationJson = this.colHash.get(col);
		String trimmed = val.trim();
		Long longVal = null;
		Double doubleVal = null;
		String t = null;
		
		
		
		// NOT_EMPTY after trimming
		if (validationJson.containsKey(KEY_NOT_EMPTY) && (boolean) validationJson.get(KEY_NOT_EMPTY)) {
			if (trimmed.length() == 0) {
				this.addError("violates notEmpty", row, col);
				return;
			}
			
		// ==== If cell is empty and empty is allowed, no other checks will be run =====
		} else {
			if (trimmed.length() == 0) {
				return;
			}
		}
		
		// REGEX_MATCHES the entire cell after trimming
		if (validationJson.containsKey(KEY_REGEX_MATCHES)) {
			if (this.patternMatches[col].matcher(trimmed).matches() == false) {
				this.addError("failed to match regex \'" + this.regexMatches[col] + "\'", row, col);
				return;
			}
			
		} 

		// REGEX_NOMATCH does not match the entire cell after trimming
		if (validationJson.containsKey(KEY_REGEX_NOMATCH)) {
			if (this.patternNoMatch[col].matcher(trimmed).matches() == true) {
				this.addError("matched regex \'" + this.regexNoMatch[col] + "\'", row, col);
				return;
			}
		}
		
		// parse if type is dictated
		if (validationJson.containsKey(KEY_TYPE)) {
			t = ((String) validationJson.get(KEY_TYPE)).trim();
			if (t.equals(VAL_TYPE_FLOAT)) {
				try {
					doubleVal = Double.valueOf(trimmed);
				} catch (Exception e) {
					this.addError("Invalid double: " + trimmed, row, col);
					return;
				}
			} else if (t.equals(VAL_TYPE_INT)) {
				try {
					longVal = Long.valueOf(trimmed);
				} catch (Exception e) {
					this.addError("Invalid integer: " + trimmed, row, col);
					return;
				}
			}
		}
		// ValidateTheJson has made already checked that if there is arithmetic,
		// then there is a type t and the operand is the correct type
		// and we parsed intVal or doubleVal above.
		if (validationJson.containsKey(KEY_ARITH_GT)) {
			if (t.equals(VAL_TYPE_FLOAT) && ! (doubleVal > (Double) validationJson.get(KEY_ARITH_GT)) ||
				t.equals(VAL_TYPE_INT)	&& ! (longVal   >   (Long) validationJson.get(KEY_ARITH_GT))       ) {
				this.addError("failed " + trimmed + " > " + String.valueOf(validationJson.get(KEY_ARITH_GT)), row, col);
				return;
			}
		}
		
		if (validationJson.containsKey(KEY_ARITH_GTE)) {
			if (t.equals(VAL_TYPE_FLOAT) && ! (doubleVal >= (Double) validationJson.get(KEY_ARITH_GTE)) ||
				t.equals(VAL_TYPE_INT)	&& ! (longVal   >=   (Long) validationJson.get(KEY_ARITH_GTE))       ) {
				this.addError("failed " + trimmed + " >= " + String.valueOf(validationJson.get(KEY_ARITH_GTE)), row, col);
				return;
			}
		}
		
		if (validationJson.containsKey(KEY_ARITH_LT)) {
			if (t.equals(VAL_TYPE_FLOAT) && ! (doubleVal < (Double) validationJson.get(KEY_ARITH_LT)) ||
				t.equals(VAL_TYPE_INT)	&& ! (longVal   <   (Long) validationJson.get(KEY_ARITH_LT))       ) {
				this.addError("failed " + trimmed + " < " + String.valueOf(validationJson.get(KEY_ARITH_LT)), row, col);
				return;
			}
		}
		
		if (validationJson.containsKey(KEY_ARITH_LTE)) {
			if (t.equals(VAL_TYPE_FLOAT) && ! (doubleVal <= (Double) validationJson.get(KEY_ARITH_LTE)) ||
				t.equals(VAL_TYPE_INT)	&& ! (longVal   <=   (Long) validationJson.get(KEY_ARITH_LTE))       ) {
				this.addError("failed " + trimmed + " <= " + String.valueOf(validationJson.get(KEY_ARITH_LTE)), row, col);
				return;
			}
		}
		
		if (validationJson.containsKey(KEY_ARITH_NE)) {
			if (t.equals(VAL_TYPE_FLOAT) && ! (doubleVal != (Double) validationJson.get(KEY_ARITH_NE)) ||
				t.equals(VAL_TYPE_INT)	&& ! (longVal   !=   (Long) validationJson.get(KEY_ARITH_NE))       ) {
				this.addError("failed " + trimmed + " != " + String.valueOf(validationJson.get(KEY_ARITH_NE)), row, col);
				return;
			}
		}
	}
	
	private void validateTheJson() throws Exception {
		for (int col : this.colHash.keySet()) {
			JSONObject j = this.colHash.get(col);
			String t = null;
			
			// if there is any arithmetic, there must be a type
			if (this.containsArithmetic(col)) {
				if (! j.containsKey(KEY_TYPE)) {
					throw new Exception("Column " + this.colNames.get(col) + " uses arithmetic comparison but has not type specified.");
				} else {
					t = ((String) j.get(KEY_TYPE)).trim();
					if (!t.equals(VAL_TYPE_FLOAT) &&  ! t.equals(VAL_TYPE_INT)) {
						throw new Exception("Column " + this.colNames.get(col) + " uses arithmetic comparison but has non-arithmetic type: " + t + ".");
					}
				}
			}
			
			// check that operands of arithmetic match type
			for (String key : new String [] { KEY_ARITH_GT, KEY_ARITH_GTE, KEY_ARITH_LT, KEY_ARITH_LTE, KEY_ARITH_NE } ) {
				if (j.containsKey(key)) {
					if (t.equals(VAL_TYPE_FLOAT)) {
						if (key.equals(KEY_ARITH_NE)) {
							throw new Exception("Column " + this.colNames.get(col) + " validation ne (not equals) is not reliable for floats. Reformulate.");
						}
						// preemptively convert long to double
						if (j.get(key) instanceof Long) {
							j.put(key, (Long) j.get(key) * 1.0);
						}
						if (! (j.get(key) instanceof Double)) {
							throw new Exception("Column " + this.colNames.get(col) + " validation " + key + "illegal operand for " + t + ": " + (String)j.get(key));
						}
					} else if (t.equals(VAL_TYPE_INT)) {
						if (! (j.get(key) instanceof Long)) {
							throw new Exception("Column " + this.colNames.get(col) + " validation " + key + "illegal operand for " + t + ": " + (String)j.get(key));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Does column validation contain arithmetic operations
	 * @param col
	 * @return
	 */
	private boolean containsArithmetic(int col) {
		JSONObject json = this.colHash.get(col);
		return json.containsKey(KEY_ARITH_GT) ||
				json.containsKey(KEY_ARITH_GTE) ||
				json.containsKey(KEY_ARITH_LT) ||
				json.containsKey(KEY_ARITH_LTE) ||
				json.containsKey(KEY_ARITH_NE);
	}
	/**
	 * Create message of form:  "row 4 colname: msg\n"
	 * @param msg
	 * @param r
	 * @param c
	 */
	private void addError(String msg, int row, int col) {
		this.errorCount ++;
		this.errors.append("row " + String.valueOf(row) + " " + this.colNames.get(col) + ": " + msg + "\n");
	}
}
