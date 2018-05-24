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

package com.ge.research.semtk.services.nodeGroupExecution;

// Paul May 2018
// This is a bad copy of the status service version.
// It needs to be fixed but somehow without breaking backwards compatibility.
public class StatusRequestBody {
	private String jobID;
	
	public void setJobID(String jobID){
		this.jobID = jobID;
	}
	public String getJobID(){
		return this.jobID;
	}
}
