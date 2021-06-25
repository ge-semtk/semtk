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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class PathRequest extends NodegroupRequest {

	
	@ApiModelProperty(
            position = 2,
            name = "addClass",
            value = "Find paths to add this class to nodegroup",
            required = true,
            example = "http://semtk#MyClass")
    private String addClass = "";
	
	@ApiModelProperty(
            position = 3,
            name = "propsInDataFlag",
            value = "Use only links where instance(s) of class->prop->class exists in instance data",
            required = false,
            example = "true")
    private Boolean propsInDataFlag = false;
	
	
	@ApiModelProperty(
            position = 4,
            name = "nodegroupInDataFlag",
            value = "Return only paths such that new nodegroup w/o constraints returns data in instance data",
            required = false,
            example = "true")
	
    private Boolean nodegroupInDataFlag = false;

	public String getAddClass() throws Exception {
		return addClass;
	}
	
	public Boolean getPropsInDataFlag() {
		return propsInDataFlag;
	}

	public Boolean getNodegroupInDataFlag() {
		return nodegroupInDataFlag;
	}

}
