/**
 ** Copyright 2017 General Electric Company
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

package com.ge.research.semtk.services.nodeGroupService.requests;

import io.swagger.v3.oas.annotations.media.Schema;

public class SetImportSpecRequest extends NodegroupRequest {

	@Schema(
            name = "action",
            required = false,
            example = "one of: \"Build from nodegroup\"")
    private String action = "";

	public String getAction() throws Exception {
		switch (this.action) {
		case "":
		case "Build from nodegroup":
			return this.action;
		default:
			 throw new Exception("Invalid action: " + this.action);
		}
	}
	
	@Schema(
            name = "lookupRegex",
            required = false,
            example = "_id")
    private String lookupRegex = "";

	public String getLookupRegex() {
		return this.lookupRegex;
	}
	
	@Schema(
            name = "lookupMode",
            required = false,
            example = "one of: \"noCreate\", \"createIfMissing\", \"errorIfExists\"")
    private String lookupMode = "";

	public String getLookupMode() throws Exception {
		switch (this.lookupMode) {
		case "":
		case "noCreate":
		case "createIfMissing":
		case "errorIfExists":
			return this.lookupMode;
		default:
			 throw new Exception("Invalid lookupMode: " + this.lookupMode);
		}
	}
}
