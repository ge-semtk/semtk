package com.ge.research.semtk.services.ingestion;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ge.research.semtk.resultSet.SimpleResultSet;

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
		catch(Exception eee){
			retval = new SimpleResultSet(false, eee.getMessage());
			eee.printStackTrace();
		}
		
		return retval.toJson();
	}
}
