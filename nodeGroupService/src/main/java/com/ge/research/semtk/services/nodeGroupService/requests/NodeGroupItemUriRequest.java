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

import javax.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

public class NodeGroupItemUriRequest extends NodegroupRequest {

	@Schema(
            name = "itemStr",
            required = true,
            example = "sparqlID|keyname")
    private String itemStr;
	
	@Schema(
            name = "newURI",
            required = true,
            example = "uri://this")
	@Pattern(regexp = "(domain|range)")
    private String newURI;
	
	@Schema(
            name = "domainOrRange",
            required = true,
            example = "domain",
            description = "range - change the range only<br>" + "domain - change domain.  If domain is valid to model, change range too if wrong.")
	@Pattern(regexp = "(domain|range)")
    private String domainOrRange;

	public String getItemStr() {
		return itemStr;
	}

	public String getNewURI() {
		return newURI;
	}

	public String getDomainOrRange() {
		return domainOrRange;
	}
	
	public boolean isDomain() {
		return domainOrRange.equals("domain");
	}
	public boolean isRange() {
		return domainOrRange.equals("range");
	}

	
}
