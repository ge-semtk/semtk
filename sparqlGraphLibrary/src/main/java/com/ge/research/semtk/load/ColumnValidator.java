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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.utility.Utility;

public class ColumnValidator {
	
	private JSONObject valJson;
	private Pattern patternMatch = null;  // patternMatches<colName> = regex pattern
	private Pattern patternNoMatch = null;  // patternMatches<colName> = regex pattern
	private String type = null;
	private Object ltParam = null;
	private Object gtParam = null;
	private Object lteParam = null;
	private Object gteParam = null;
	private Object neParam = null;
	
	static final String KEY_COL_NAME = "colName";
	
	// supported validations
	static final String KEY_NOT_EMPTY = "notEmpty";
	static final String KEY_REGEX_MATCHES = "regexMatches";
	static final String KEY_REGEX_NOMATCH = "regexNoMatch";
	
	static final String KEY_TYPE = "type";
	
	static final String KEY_ARITH_LT = "lt";
	static final String KEY_ARITH_GT = "gt";
	static final String KEY_ARITH_LTE = "lte";
	static final String KEY_ARITH_GTE = "gte";
	static final String KEY_ARITH_NE = "ne";
	
	// types
	static final String VAL_TYPE_INT = "int";
	static final String VAL_TYPE_FLOAT = "float";
	static final String VAL_TYPE_DATE = "date";
	static final String VAL_TYPE_TIME = "time";
	static final String VAL_TYPE_DATETIME = "datetime";

	/**
	 * 
	 * @param ds
	 * @param colJsonArr
	 * @throws Exception  - if colJsonArr is not valid
	 */
	public ColumnValidator(JSONObject j) throws Exception {
		
		this.validateTheJson(j);
		this.valJson = j;
		
		// hash regex patterns
		String regex = (String) j.get(KEY_REGEX_MATCHES);
		if (regex != null) {
			this.patternMatch = Pattern.compile(regex);
			
		} 
		regex = (String) j.get(KEY_REGEX_NOMATCH);
		if (regex != null) {
			this.patternNoMatch = Pattern.compile(regex);
		} 
		
		this.type = (String) j.get(KEY_TYPE);
		
		if (this.type != null) {
			// these will be objects or null
			Object gt = j.get(KEY_ARITH_GT);
			Object lt = j.get(KEY_ARITH_LT);
			Object gte = j.get(KEY_ARITH_GTE);
			Object lte = j.get(KEY_ARITH_LTE);
			Object ne = j.get(KEY_ARITH_NE);

			if (this.type.equals(VAL_TYPE_INT)) {
				this.gtParam =   gt==null ? null: (Long) gt;
				this.ltParam =   lt==null ? null: (Long) lt;
				this.gteParam = gte==null ? null: (Long) gte;
				this.lteParam = lte==null ? null: (Long) lte;
				this.neParam =   ne==null ? null: (Long) ne;

			} else if (this.type.equals(VAL_TYPE_FLOAT)) {
				this.gtParam =   gt==null ? null: (Double) gt;
				this.ltParam =   lt==null ? null: (Double) lt;
				this.gteParam = gte==null ? null: (Double) gte;
				this.lteParam = lte==null ? null: (Double) lte;
				this.neParam =   ne==null ? null: (Double) ne;

			} else if (this.type.equals(VAL_TYPE_DATE) ) {
				this.gtParam =   gt==null ? null: LocalDate.parse((String) gt);
				this.ltParam =   lt==null ? null: LocalDate.parse((String) lt);
				this.gteParam = gte==null ? null: LocalDate.parse((String) gte);
				this.lteParam = lte==null ? null: LocalDate.parse((String) lte);
				this.neParam =   ne==null ? null: LocalDate.parse((String) ne);
				
			} else if (this.type.equals(VAL_TYPE_TIME) ) {
				this.gtParam =   gt==null ? null: LocalTime.parse((String) gt);
				this.ltParam =   lt==null ? null: LocalTime.parse((String) lt);
				this.gteParam = gte==null ? null: LocalTime.parse((String) gte);
				this.lteParam = lte==null ? null: LocalTime.parse((String) lte);
				this.neParam =   ne==null ? null: LocalTime.parse((String) ne);
				
			} else if (this.type.equals(VAL_TYPE_DATETIME) ) {
				this.gtParam =   gt==null ? null: LocalDateTime.parse((String) gt);
				this.ltParam =   lt==null ? null: LocalDateTime.parse((String) lt);
				this.gteParam = gte==null ? null: LocalDateTime.parse((String) gte);
				this.lteParam = lte==null ? null: LocalDateTime.parse((String) lte);
				this.neParam =   ne==null ? null: LocalDateTime.parse((String) ne);
			}
		}
			
	}
	
	public String getColumnName() {
		return (String) this.valJson.get(KEY_COL_NAME);
	}
	
	/**
	 * Returns arrayList of Errors
	 * @param val
	 * @param row
	 * @param col
	 */
	public ArrayList<String> validateCell(String val) throws Exception {
		String trimmed = val.trim();
		Long longVal = null;
		Double doubleVal = null;
		String t = null;
		ArrayList<String> ret = new ArrayList<String>();
		
		// NOT_EMPTY after trimming
		if (this.isNonEmpty()) {
			if (trimmed.length() == 0) {
				ret.add(this.buildError("violates notEmpty"));
				return ret;
			}
			
		// ==== If cell is empty and empty is allowed, no other checks will be run =====
		} else {
			if (trimmed.length() == 0) {
				return ret;
			}
		}
		
		// REGEX_MATCHES the entire cell after trimming
		String regex = (String) this.valJson.get(KEY_REGEX_MATCHES);
		if (regex != null) {
			if (this.patternMatch.matcher(trimmed).matches() == false) {
				ret.add(this.buildError("failed to match regex \'" + regex + "\'"));
				return ret;
			}
			
		} 

		// REGEX_NOMATCH does not match the entire cell after trimming
		regex = (String) this.valJson.get(KEY_REGEX_NOMATCH);
		if (this.valJson.containsKey(KEY_REGEX_NOMATCH)) {
			if (this.patternNoMatch.matcher(trimmed).matches() == true) {
				ret.add(this.buildError("matched regex \'" + regex + "\'"));
				return ret;
			}
		}
		
		Object colTypedVal = null;
		
		// parse if type is dictated
		if (this.type != null) {
			if (this.type.equals(VAL_TYPE_FLOAT)) {
				try {
					colTypedVal = Double.valueOf(trimmed);
				} catch (Exception e) {
					ret.add(this.buildError("Invalid double: " + trimmed));
					return ret;
				}
			} else if (this.type.equals(VAL_TYPE_INT)) {
				try {
					colTypedVal = Long.valueOf(trimmed);
				} catch (Exception e) {
					ret.add(this.buildError("Invalid integer: " + trimmed));
					return ret;
				}
			} else if (this.type.equals(VAL_TYPE_DATE)) {
				try {
					String semtkDate = Utility.getSPARQLDateString(trimmed);
					colTypedVal = LocalDate.parse(semtkDate);
				} catch (Exception e) {
					ret.add(this.buildError("Invalid date: " + trimmed));
					return ret;
				}
			} else if (this.type.equals(VAL_TYPE_TIME)) {
				try {
					XSDSupportedType.TIME.validate(trimmed);
					colTypedVal = LocalTime.parse(trimmed);
				} catch (Exception e) {
					ret.add(this.buildError("Invalid time: " + trimmed));
					return ret;
				}
			} else if (this.type.equals(VAL_TYPE_DATETIME)) {
				try {
					String semtkDateTime = Utility.getSPARQLDateTimeString(trimmed);
					colTypedVal = LocalDateTime.parse(semtkDateTime);
				} catch (Exception e) {
					ret.add(this.buildError("Invalid datetime: " + trimmed));
					return ret;
				}
			}
		}
		
		// ValidateTheJson has made already checked that if there is arithmetic,
		// then there is a type t and the operand is the correct type
		// and we parsed intVal or doubleVal above.
		if (this.gtParam != null) {
			if (this.myCompare(colTypedVal, this.gtParam) <= 0) {
				ret.add(this.buildError("failed " + trimmed + " > " + String.valueOf(this.gtParam)));
				return ret;
			}
		}
		
		if (this.gteParam != null) {
			if (this.myCompare(colTypedVal, this.gteParam) < 0 ) {
				ret.add(this.buildError("failed " + trimmed + " >= " + String.valueOf(this.valJson.get(KEY_ARITH_GTE))));
				return ret;
			}
		}
		
		if (this.ltParam != null) {
			if (this.myCompare(colTypedVal, this.ltParam) >= 0 ) {
				ret.add(this.buildError("failed " + trimmed + " < " + String.valueOf(this.valJson.get(KEY_ARITH_LT))));
				return ret;
			}
		}
		
		if (this.lteParam != null) {
			if (this.myCompare(colTypedVal, this.lteParam) > 0 ) {   
				ret.add(this.buildError("failed " + trimmed + " <= " + String.valueOf(this.valJson.get(KEY_ARITH_LTE))));
				return ret;
			}
		}
		
		if (this.neParam != null) {
			if (this.myCompare(colTypedVal, this.neParam) == 0 ) {   
				ret.add(this.buildError("failed " + trimmed + " != " + String.valueOf(this.valJson.get(KEY_ARITH_NE))));
				return ret;
			}
		}
		return ret;
	}
	
	private int myCompare(Object o1, Object o2) throws Exception {
		if (o1 instanceof Long) {
			return ((Long) o1).compareTo((Long) o2);
		} else if (o1 instanceof Double) {
			return ((Double) o1).compareTo((Double) o2);
		} else if (o1 instanceof LocalDate) {
			return ((LocalDate) o1).compareTo((LocalDate) o2);
		} else if (o1 instanceof LocalTime) {
			return ((LocalTime) o1).compareTo((LocalTime) o2);
		} else if (o1 instanceof LocalDateTime) {
			return ((LocalDateTime) o1).compareTo((LocalDateTime) o2);
		}
		throw new Exception("Internal error: bad type: " + o1.toString());
	}
	
	private void validateTheJson(JSONObject j) throws Exception {
		String colName = (String) j.get(KEY_COL_NAME);

		if (colName == null) {
			throw new Exception("Validation array entry has no column name.");
		}

		String t = null;

		// if there is any arithmetic, there must be a type
		if (this.containsArithmetic(j)) {
			if (! j.containsKey(KEY_TYPE)) {
				throw new Exception("Column " + colName + " uses arithmetic comparison but has not type specified.");
			} else {
				t = ((String) j.get(KEY_TYPE)).trim();
				if (!t.equals(VAL_TYPE_FLOAT) &&  ! t.equals(VAL_TYPE_INT)  &&  ! t.equals(VAL_TYPE_DATE) &&  ! t.equals(VAL_TYPE_TIME) &&  ! t.equals(VAL_TYPE_DATETIME)) {
					throw new Exception("Column " + colName + " uses arithmetic comparison but has non-arithmetic type: " + t + ".");
				}
			}
		}

		// check that operands of arithmetic match type
		for (String key : new String [] { KEY_ARITH_GT, KEY_ARITH_GTE, KEY_ARITH_LT, KEY_ARITH_LTE, KEY_ARITH_NE } ) {
			if (j.containsKey(key)) {
				if (t.equals(VAL_TYPE_FLOAT)) {
					if (key.equals(KEY_ARITH_NE)) {
						throw new Exception("Column " + colName + " validation ne (not equals) is not reliable for floats. Reformulate.");
					}
					// preemptively convert long to double
					if (j.get(key) instanceof Long) {
						j.put(key, (Long) j.get(key) * 1.0);
					}
					if (! (j.get(key) instanceof Double)) {
						throw new Exception("Column " + colName + " validation " + key + "illegal operand for " + t + ": " + (String)j.get(key));
					}
				} else if (t.equals(VAL_TYPE_INT)) {
					if (! (j.get(key) instanceof Long)) {
						throw new Exception("Column " + colName + " validation " + key + "illegal operand for " + t + ": " + (String)j.get(key));
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
	public boolean containsArithmetic(JSONObject json) {
		return json.containsKey(KEY_ARITH_GT) ||
				json.containsKey(KEY_ARITH_GTE) ||
				json.containsKey(KEY_ARITH_LT) ||
				json.containsKey(KEY_ARITH_LTE) ||
				json.containsKey(KEY_ARITH_NE);
	}
	
	public boolean isNonEmpty() {
		return (this.valJson.containsKey(KEY_NOT_EMPTY) && (boolean) this.valJson.get(KEY_NOT_EMPTY));		
	}
	
	/**
	 * Create message of form:  "row 4 colname: msg\n"
	 * @param msg
	 * @param r
	 * @param c
	 */
	private String buildError(String msg) {
		return "column " + this.getColumnName() + " failed validation: " + msg;
	}
}
