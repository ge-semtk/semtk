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


package com.ge.research.semtk.springutilib.requests;


/* contains no template */

public class IngestConstants {
	public static final String ASYNC_NOTES = "Success returns a jobId.\n" +
			"* check for that job's status of success with status message" +
			"* if job's status is failure then fetch a results table with ingestion errors" + 
			"Failure can return a rationale explaining what prevented the ingestion or precheck from starting.";
	
}
