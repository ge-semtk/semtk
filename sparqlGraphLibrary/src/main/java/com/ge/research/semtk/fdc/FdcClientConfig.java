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
package com.ge.research.semtk.fdc;

import java.util.Arrays;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.client.RestClientConfig;

public class FdcClientConfig extends RestClientConfig {
	HashMap<String, Table> tableHash = null;
	String nodegroupId = null;
	
	/**
	 * Create a "normal" fdc call client
	 * @param protocol
	 * @param host
	 * @param port
	 * @param endpoint
	 * @param m
	 * @param tableHashMap
	 * @throws Exception
	 */
	FdcClientConfig(String protocol, String host, int port, String endpoint, Methods m, HashMap<String, Table> tableHashMap) throws Exception {
		super(protocol, host, port, endpoint, m);
		this.tableHash = tableHashMap;
		this.nodegroupId = null;
	}
	
	/**
	 * Create a "getNodegroup" client
	 * @param protocol
	 * @param host
	 * @param port
	 * @param endpoint
	 * @param m
	 * @param nodegroupId
	 * @throws Exception
	 */
	FdcClientConfig(String protocol, String host, int port, String endpoint, Methods m, String nodegroupId) throws Exception {
		super(protocol, host, port, endpoint, m);
		this.tableHash = null;
		this.nodegroupId = nodegroupId;
	}
	
	public String getNodegroupId() {
		return nodegroupId;
	}

	public boolean isGetNodegroup() {
		return this.nodegroupId != null;
	}
	
	public static FdcClientConfig fromFullEndpoint(String fullEndpoint, HashMap<String, Table> tableHashMap) throws Exception {
		String [] parts = fullEndpoint.split("[:/]+");
		String protocol = parts[0];
		String host =     parts[1];
		int port =        Integer.parseInt(parts[2]);
		String ending   = String.join("/", Arrays.copyOfRange(parts, 3, parts.length));
		return new FdcClientConfig(protocol, host, port, ending, Methods.POST, tableHashMap);
	}

	
	public static FdcClientConfig buildGetNodegroup(String fullEndpoint, String id) throws Exception {
		String [] parts = fullEndpoint.split("[:/]+");
		String protocol = parts[0];
		String host =     parts[1];
		int port =        Integer.parseInt(parts[2]);
		String ending   = "/" + parts[3] + "/getNodegroup";
		return new FdcClientConfig(protocol, host, port, ending, Methods.POST, id);
	}
	
	public JSONObject getTableHashJson() throws Exception {
		
		// convert this.tableHash into json
		JSONObject tableJson = new JSONObject();
		for (Object k : this.tableHash.keySet()) {
			tableJson.put((String) k, (this.tableHash.get((String) k)).toJson());
		}
		
		return tableJson;
	}
}
