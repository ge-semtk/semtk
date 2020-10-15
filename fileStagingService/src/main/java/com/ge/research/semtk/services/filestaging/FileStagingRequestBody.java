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

package com.ge.research.semtk.services.filestaging;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ge.research.semtk.springutilib.requests.JobIdRequest;

import io.swagger.annotations.ApiModelProperty;

/**
 * For service calls to file staging service
 */
public class FileStagingRequestBody extends JobIdRequest{
	
	private final static String DELIMITER = "###"; // delimiter for the "query" (e.g. "dir/test1-98231834.img###test1.img")
	
	@Pattern(regexp="###", message="query is missing the ### delimiter")
	@ApiModelProperty(
			value = "\"query\"",
			required = true,
			example = "test1-98231834.img###test1.img")   // The first element is the key (omit s3://bucketname).  The second element is the file name to use for the staged file.
    public String query;  	// a URL and filename
    
    /**
     * Validate request contents.  Throws an exception if validation fails.
     */
    public void validate() throws Exception{
		if(query == null){
			throw new Exception("Error: missing query");
		}
		if(query.split(DELIMITER).length != 2){
			throw new Exception("Input is not in format \"URL" + DELIMITER + "filename\": '" + query + "'");
		}
    }
    
    /**
     * Parse the source file from the query 
     */
    @JsonIgnore // else shows up as a field
    public String getSourceFile(){
    	return query.split(DELIMITER)[0];
    }
    
    /**
     * Parse the stage file name from the query 
     */
    @JsonIgnore  // else shows up as a field
    public String getStageFilename(){
    	return query.split(DELIMITER)[1];	
    }
}
