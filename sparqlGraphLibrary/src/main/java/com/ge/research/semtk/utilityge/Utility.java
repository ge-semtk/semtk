/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.utilityge;

/*
 * Utility fields and methods
 */
public abstract class Utility {
	
	// column name used in multiple modules
	public static final String COL_NAME_UUID = "UUID";
	
	// column names used for time series query generator input
	public static final String COL_NAME_VARIABLE_NAME = "VarName";
	public static final String COL_NAME_TAG_NAME = "TagName";
	public static final String COL_NAME_TAG_PREFIX = "TagPrefix"; // used for KairosDB
	public static final String COL_NAME_TABLE_NAME = "TableName";
	public static final String COL_NAME_TIMESERIES_TABLE_TYPE = "TimeSeriesTableType";
	public static final String COL_NAME_DATABASE_SERVER = "DatabaseServer";
	public static final String COL_NAME_DATABASE = "Database";
	public static final String COL_NAME_TIMESTAMP_COLUMN = "TimeStampColumnList";  // note this needs to be a single column name (keeping this header for legacy reasons)  	

	// column names used for binary file query generator input
	public static final String COL_NAME_URL = "URL";
	public static final String COL_NAME_FILENAME = "FileName";
	
	// column names used for binary file stageFile output
	public static final String COL_NAME_FILEID = "fileID";    // lower case to match normal SADL convention
	
	// column names used for query executor input
	public static final String COL_NAME_QUERY = "Query";
	public static final String COL_NAME_CONFIGJSON = "ConfigJSON";
	
}
