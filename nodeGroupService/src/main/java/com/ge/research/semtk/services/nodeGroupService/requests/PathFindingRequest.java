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

import com.ge.research.semtk.springutilib.requests.NodegroupRequest;

import io.swagger.v3.oas.annotations.media.Schema;

public class PathFindingRequest extends NodegroupRequest {

	
	@Schema(
            name = "addClass",
            description = "Find paths to add this class to nodegroup",
            required = true,
            example = "http://semtk#MyClass")
    private String addClass = "";
	
	@Schema(
            name = "propsInDataFlag",
            		description = "Use only links where instance(s) of class->prop->class exists in instance data",
            required = false,
            example = "true")
    private Boolean propsInDataFlag = false;
	
	
	@Schema(
            name = "nodegroupInDataFlag",
            		description = "Return only paths such that new nodegroup w/o constraints returns data in instance data",
            required = false,
            example = "true")
    private Boolean nodegroupInDataFlag = false;
	
	@Schema(
            name = "maxLengthRange",
            description = "Stop when longest path is this much longer than shortest",
            required = false,
            example = "5")
	private int maxLengthRange = 5;
	
	@Schema(
            name = "maxTimeMsec",
            description = "Stop after this many milliseconds of path-finding",
            required = false,
            example = "5000")
	private int maxTimeMsec = 5000;
	
	@Schema(
            name = "maxPathLength",
            description = "Stop when all paths this length or shorter have been found",
            required = false,
            example = "10")
	private int maxPathLength = 10;
	
	@Schema(
            name = "maxPathCount",
            description = "Stop when this many paths have been found",
            required = false,
            example = "100")
	private int maxPathCount = 100;

	public String getAddClass() throws Exception {
		return addClass;
	}
	
	public Boolean getPropsInDataFlag() {
		return propsInDataFlag;
	}

	public Boolean getNodegroupInDataFlag() {
		return nodegroupInDataFlag;
	}

	public int getMaxLengthRange() {
		return maxLengthRange;
	}

	public int getMaxTimeMsec() {
		return maxTimeMsec;
	}

	public int getMaxPathLength() {
		return maxPathLength;
	}

	public int getMaxPathCount() {
		return maxPathCount;
	}

	
}
