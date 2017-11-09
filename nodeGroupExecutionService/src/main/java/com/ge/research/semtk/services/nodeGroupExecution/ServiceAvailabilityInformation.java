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


package com.ge.research.semtk.services.nodeGroupExecution;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.utility.LocalLogger;

@CrossOrigin
@RestController
@RequestMapping("/serviceInfo")
public class ServiceAvailabilityInformation {

	
	@CrossOrigin
	@RequestMapping(value="/ping", method= RequestMethod.POST)
	public JSONObject ping(){
		
		SimpleResultSet retval = null;
		
		try{
			retval = new SimpleResultSet(true);
			retval.addResult("available", "yes");
			
		}
		catch(Exception e){
			retval = new SimpleResultSet(false, e.getMessage());
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();
	}
}
