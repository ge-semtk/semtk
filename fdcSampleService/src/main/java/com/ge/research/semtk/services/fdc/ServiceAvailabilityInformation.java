package com.ge.research.semtk.services.fdc;

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
