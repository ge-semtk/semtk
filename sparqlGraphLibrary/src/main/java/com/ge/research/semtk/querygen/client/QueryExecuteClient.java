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


package com.ge.research.semtk.querygen.client;

import com.ge.research.semtk.edc.client.ExecuteClientConfig;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;

/**
 * Client to execute queries
 */
public abstract class QueryExecuteClient extends RestClient {
	private String jobId = null;
	
	public QueryExecuteClient(ExecuteClientConfig conf){
		this.conf = conf;
	}
	
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	public String getJobId() {
		return this.jobId;
	}
	
	public abstract TableResultSet execute(String query) throws Exception;
	
}
