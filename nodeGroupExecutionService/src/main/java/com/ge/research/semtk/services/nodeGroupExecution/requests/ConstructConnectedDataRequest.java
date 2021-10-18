/**
 ** Copyright 2018 General Electric Company
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

package com.ge.research.semtk.services.nodeGroupExecution.requests;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel
public class ConstructConnectedDataRequest extends SparqlConnectionRequest {
    
    @ApiModelProperty(
    		value = "instanceVal",
            required = true,
            example = "http://path#this")
    private String instanceVal;
    
    @ApiModelProperty(
    		value = "instanceType",
            required = false,
            example = "node_uri")
    private String instanceType = null;

 
    public String getInstanceVal() {
		return instanceVal;
	}
    
    public String getInstanceType() {
		return instanceType;
	}

	public XSDSupportedType buildInstanceType() throws Exception {
        return this.instanceType == null ? null :  XSDSupportedType.getMatchingValue(this.instanceType);
    }

}
