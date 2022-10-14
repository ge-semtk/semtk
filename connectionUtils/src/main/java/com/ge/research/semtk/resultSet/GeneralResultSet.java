/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.resultSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.utility.LocalLogger;

public abstract class GeneralResultSet {

	public final static String SUCCESS = "success";
	public final static String FAILURE = "failure";
	
	private final static String SuccessMessage = "operations succeeded.";
	private final static String FailureMessage = "operations failed.";
	private final static String AmbiguousMessage = "operations success/failure unknown.";
	
	protected JSONObject resultsContents; // this is what will, ultimately be returned
	protected Boolean success;			// this indicates whether the operation was successful or not
	protected ArrayList<String> rationale;// this holds information related to runtime exceptions, error info, etc.
	
	public GeneralResultSet(){
		this.rationale = new ArrayList<String>();
	}
	
	public GeneralResultSet(Boolean succeeded){
		if(succeeded != null){
			this.success = succeeded;
		}
		this.rationale = new ArrayList<String>();
	}
		
	// get the JSON key used to store the results block.
	public abstract String getResultsBlockName();
	
	public boolean getSuccess(){
		return success;		
	}
	
	// -Paul 9/13/2017
	// Use this only for cases where it makes to sense to indicate the calling method or rest service
	//
	// If at all possible USE ONE OF THE OTHER VARIANTS
	public void addRationaleMessage(String avoid_where_possible_msg){
		this.rationale.add(avoid_where_possible_msg);
	}
	
	// for NON-exceptions from services
	public void addRationaleMessage(String serviceName, String endpoint, String message) {
		this.rationale.add(String.format("%s/%s error: %s", serviceName, endpoint, message));
	}
	
	// for NON-exceptions from some useful source
	public void addRationaleMessage(String source, String message) {
		this.rationale.add(source + ": " + message);
	}
	
	// for EXCEPTIONS from Rest Clients
	public void addRationaleMessage(String serviceName, String endpoint, Exception e) {
		this.addRationaleMessage(serviceName + "/" + endpoint,  e);
	}
		
	// for EXCEPTIONS with generic source (e.g. "Object.method()" )
	public void addRationaleMessage(String source, Exception e) {
		// build a message
		StackTraceElement [] trace = e.getStackTrace();
		String msg = "";
		
		// if stack trace is small, use e.toString().  Otherwise get first few lines.
		if (trace.length < 2) {
			msg = e.toString();
		} else {
			msg = e.getMessage() + "\n" + trace[0].toString() + "\n" + trace[1].toString() + "\n" + trace[2].toString() + "\n...";
		}
		
		this.rationale.add(String.format("%s threw %s %s", source, e.getClass().getName(), msg));
	}
	/**
	 * Error is intended for user, not developer
	 * @param e
	 */
	public void addOnlyRationaleMessage(Exception e) {
		this.rationale.add(e.getMessage());
	}
	
	public String getRationaleAsString(String delimiter){
		String retval = "";
		
		// spin through the array list and return the elements. delimit them by something.
		for(String excuse : this.rationale){
			retval += excuse + delimiter;
		}
		if(retval.endsWith("||")){
			retval = retval.substring(0, retval.length() - 2);
		}
		return retval;
	}
	
	public void setSuccess(Boolean successful){
		this.success = successful;
	}
	
	public String getResultCodeString(){
		// do we have results?
		if(this.success == null){ return GeneralResultSet.AmbiguousMessage; }
		if(this.success){ return GeneralResultSet.SuccessMessage; }
		else{ return GeneralResultSet.FailureMessage; }
	}
	
	public void throwExceptionIfUnsuccessful () throws AuthorizationException, Exception {
		this.throwExceptionIfUnsuccessful("");
	}
	
	public void throwExceptionIfUnsuccessful (String msg) throws AuthorizationException, Exception {
		if (success != true) {
			String rationale = this.getRationaleAsString("\n");
			String fullMessage = (msg != null && !msg.isEmpty()) ? msg + "\n" + rationale : rationale;
			
			if (rationale.contains("threw com.ge.research.semtk.auth.AuthorizationException")) {
				throw new AuthorizationException(fullMessage);
			} else {
				throw new Exception(fullMessage);
			}
		}
	}
	
	/**
	 * Add result content as JSON
	 */
	public void addResultsJSON(JSONObject results) {
		this.resultsContents = results;
	}	
	
	/**
	 * Get result contents as JSON
	 */
	public JSONObject getResultsJSON(){
		return resultsContents;
	}
	
	/**
	 * Get result content as an Object (e.g. for NodeGroupResultSet, override to return a NodeGroup)
	 * @throws Exception 
	 */
	public abstract Object getResults() throws Exception;
	
	
	/**
	 * Create a GeneralResultSet from JSON
	 */
	public void readJson(JSONObject jsonObj) {
		
		// check the json to see if it looks like "we" generated it or not
		if (jsonObj.containsKey("error") || !jsonObj.containsKey("message")) {
			// Json was not generated by our microservice.   Presumably swagger or something else running on this port.
			
			if (jsonObj.containsKey("status") && jsonObj.containsKey("error")) {
				this.rationale.add(String.format("\tstatus %s: %s\n", jsonObj.get("status").toString(), jsonObj.get("error")));
				jsonObj.remove("status");
				jsonObj.remove("error");
			}
	
			// important known keys first
			for (String key : new String[]{"exception", "message"}) {
				if (jsonObj.containsKey(key)) {
					this.rationale.add(String.format("\t%s: %s\n", key, jsonObj.get(key).toString()));
					jsonObj.remove(key);
				}
			}
			// tack on the rest
			for (Object k : jsonObj.keySet()) {
				this.rationale.add(String.format("\t%s: %s\n", k.toString(), jsonObj.get(k).toString()));
			}
			this.success = false;
			
		} else {
			String s;
			try {
				s = jsonObj.get("status").toString();
			} catch (Exception e) {
				s = "";
			}
			if (s.equals(GeneralResultSet.SUCCESS) || s.equals(GeneralResultSet.FAILURE)) {
				success = s.equals(GeneralResultSet.SUCCESS);
				// subclass has set resultsBlockName
				String blockName = this.getResultsBlockName();
				if (jsonObj.containsKey(blockName)) {
					resultsContents = (JSONObject) jsonObj.get(blockName);
				}
				
				// add rationale if any, regardless of success
				if (jsonObj.containsKey("rationale")) {
					String fullRationale = (String)jsonObj.get("rationale");
					rationale = new ArrayList<String>( Arrays.asList(fullRationale.split(Pattern.quote("||"))) );
					
				} else {
					rationale = new ArrayList<String>();
				}
			} else {
				success = false;
				resultsContents = null;
				rationale = new ArrayList<String>();
				rationale.add("Invalid or missing success/failure.");
				rationale.addAll( Arrays.asList(jsonObj.toJSONString().split("[\n]+")));
			}
			
		}
	}		
	
	/*
	abstract public static GeneralResultSet fromJson(JSONObject jsonObj);
	*/
	
	/**
	 * Convert the GeneralResultSet to JSON
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJson(){
		JSONObject retval = new JSONObject();
		
		if(this.success == null){  // the status is unknown. return of results called before values set.
			retval.put("status", GeneralResultSet.FAILURE);
			retval.put("message", GeneralResultSet.AmbiguousMessage);
			
			this.addRationaleMessage(AmbiguousMessage);
		}
		else if(success){
			retval.put("status", GeneralResultSet.SUCCESS);
			retval.put("message", GeneralResultSet.SuccessMessage);
		}
		else{
			retval.put("status", GeneralResultSet.FAILURE);
			retval.put("message", GeneralResultSet.FailureMessage);
		}
		
		String rationaleString = this.getRationaleAsString("||");
		if(rationaleString.length() > 0){
			retval.put("rationale", rationaleString);
		}
		
		// in any case, if the results are not null, let's include them. 
		// partial results may be meaningful to some entities.
		if(this.resultsContents != null && getResultsBlockName() != null && getResultsBlockName().length() > 0){
			retval.put(getResultsBlockName(), this.resultsContents);
		}
		
		return retval;	// return the JSONObject
	}	
	
}
