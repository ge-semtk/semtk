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

package com.ge.research.semtk.services.storedNodegroupExecution;

public class DispatchByIdRequestBody {
	private String nodeGroupId;
	private String sparqlConnection;
	private String externalDataConnectionConstraints;
	private String runtimeConstraints;
	
	public void setNodeGroupId(String nodeGroupId){
		this.nodeGroupId = nodeGroupId;
	}
	public String getNodeGroupId(){
		return this.nodeGroupId;
	}
	public String getSparqlConnection() {
		return sparqlConnection;
	}
	public void setSparqlConnection(String sparqlConnection) {
		this.sparqlConnection = sparqlConnection;
	}
	public String getExternalDataConnectionConstraints() {
		return externalDataConnectionConstraints;
	}
	public void setExternalDataConnectionConstraints(String externalDataConnectionConstraints) {
		this.externalDataConnectionConstraints = externalDataConnectionConstraints;
	}
	public String getRuntimeConstraints(){
		return(this.runtimeConstraints);
	}
	public void setRuntimeConstraints(String runtimeConstraints){
		this.runtimeConstraints = runtimeConstraints;
	}
	
}
