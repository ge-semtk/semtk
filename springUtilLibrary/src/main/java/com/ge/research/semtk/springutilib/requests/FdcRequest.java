/**
 ** Copyright 2017-2018 General Electric Company
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

package com.ge.research.semtk.springutilib.requests;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.tomcat.util.buf.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.resultSet.Table;

import io.swagger.annotations.ApiModelProperty;

public class FdcRequest {

	@ApiModelProperty(
		value = "tables",
		required = true,
		example = "\"{\"1\": {\"col_names\":[\"aircraftUri\",\"tailNumber\"],\"rows\":[[\"http://uri\",\"007a\"]],\"col_type\":[\"String\",\"String\"],\"col_count\":1,\"row_count\":1}}\""
					
	)	
	private String tables;

	public String getTables() {
		return tables;
	}

	public void setTables(String t) {
		this.tables = t;
	}
	
	/**
	 * Parse tables param into HashMap<paramSetString, Table>
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, Table> buildTableHash() throws Exception {
		HashMap<String, Table> ret = new HashMap<String,Table>();
		
		JSONObject j = (JSONObject) (new JSONParser()).parse(this.tables);
		for (Object key : j.keySet()) {
			String paramSetName = (String) key;
			JSONObject tableJson = (JSONObject) j.get(key);
			ret.put(paramSetName, Table.fromJson(tableJson));
		}
		return ret;
	}
	
	/**
	 * Thow exception if table hash doesn't contain paramSet with all given params
	 * @param hash
	 * @param paramSet
	 * @param params
	 * @throws Exception
	 */
	public static void validateTableHash(HashMap<String,Table> hash, String paramSet, String [] params) throws Exception {
		if (!hash.containsKey(paramSet)) {
			throw new Exception("Missing FDC param set: " + paramSet);
		}
		
		ArrayList<String> missingParams = new ArrayList<String>();
		
		Table tab = hash.get(paramSet);
		for (String param : params) {
			if (tab.getColumnIndex(param) < 0) {
				missingParams.add(param);
			}
		}
		
		if (missingParams.size() > 0) {
			throw new Exception("Missing FDC param(s) in param set " + paramSet + " :" + StringUtils.join(missingParams,','));
		}
	}
	
}
