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

package com.ge.research.semtk.services.fdccache;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.resultSet.Table;

import io.swagger.v3.oas.annotations.media.Schema;

public class TableBootstrapRequest extends FdcRequest {
	
	@Schema(
			name = "bootstrapTableJsonStr",
			required = true,
			example = "\"{\"col_names\":[\"aircraftUri\",\"tailNumber\"],\"rows\":[[\"http://uri\",\"007a\"]],\"col_type\":[\"String\",\"String\"],\"col_count\":1,\"row_count\":1}\"")
	private String bootstrapTableJsonStr;
	
	@Schema(
			name = "recacheAfterSec",
			required = false,
			example = "300")
	private int recacheAfterSec = 0;
	

	public String getBootstrapTableJsonStr() {
		return this.bootstrapTableJsonStr;
	}
	public Table buildBootstrapTable() throws Exception {
		return Table.fromJson((JSONObject) (new JSONParser()).parse(this.bootstrapTableJsonStr));
	}

	public int getRecacheAfterSec() {
		return recacheAfterSec;
	}

	/**
	 * Validate request contents.  Throws an exception if validation fails.
	 */
	public void validate() throws Exception{
		
		// specId handled with Spring
		
	}
}
