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

import com.ge.research.semtk.sparqlX.SparqlConnection;

import io.swagger.annotations.ApiModelProperty;

public class SparqlConnectionRequest {

	@ApiModelProperty(
		value = "conn",
		required = true,
		example = "{\"name\": \"%NODEGROUP%\",\"domain\": \"%NODEGROUP%\",\"model\": [],\"data\": []}"
	)	
	private String conn;
	
	/**
	 * Get just the nodegroup out of the json
	 * @return
	 * @throws Exception
	 */
	public SparqlConnection getSparqlConnection() throws Exception {
		SparqlConnection ret = new SparqlConnection();
		ret.fromString(this.conn);
		return ret;
	}

	public String getConn() {
		return conn;
	}

	public void setConn(String conn) {
		this.conn = conn;
	}
	
}
