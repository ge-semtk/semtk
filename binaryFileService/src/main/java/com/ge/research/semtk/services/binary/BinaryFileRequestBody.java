/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.services.binary;

import javax.validation.constraints.Pattern;

import com.ge.research.semtk.springutilib.requests.JobIdRequest;

import io.swagger.annotations.ApiModelProperty;

/**
 * For service calls to binary file executor 
 */
public class BinaryFileRequestBody extends JobIdRequest{
	@Pattern(regexp="###", message="query is missing the ### delimeter")
	@ApiModelProperty(
			value = "\"query\"",
			required = true,
			example = "hdfs://test1-98231834.img###test1.img")
    public String query;  	// a URL and filename
    
    /**
     * Validate request contents.  Throws an exception if validation fails.
     */
    public void validate() throws Exception{
		if(query == null){
			throw new Exception("Error: missing query");
		}
    }
}
