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


package com.ge.research.semtk.load.utility;

import com.ge.research.semtk.ontologyTools.OntologyInfo;

public class UriResolver {
	// this class will be used to resolve the URIs for a few use cases. (using oInfo)
	// 1. it will take a local fragment from the loader and return a URI prefix if it is the trailing
	//	  section of an enumeration.
	// 2. it will apply the user's personal prefix (given from the template, if provided and the value
	//    was not part of an enum.
	// 3. in the long term, it may look up values required to pre-exist and resolve them to the proper 
	//    URI.
	// in any case, it would be nice to have a one-stop location for this sort of task.

	// default uri 
	public final static String DEFAULT_URI_PREFIX = "belmont/generateSparqlInsert#";
	
	private String userUriPrefix = "";
	private OntologyInfo oInfo = null;
	
	// constructor 
	public UriResolver(String userUri, OntologyInfo oInf){
		if(userUri != null && userUri.length() > 0 && userUri != ""){
			userUri = userUri + "#";
		}
		
		this.userUriPrefix = userUri;
		this.oInfo = oInf;
	}
	
	public String getUriPrefix() {
		if (userUriPrefix == null || userUriPrefix.isEmpty()) {
			return DEFAULT_URI_PREFIX;
		} else {
			return userUriPrefix;
		}
	}
	
	public String getInstanceUriWithPrefixPREV(String classUri, String localFragment) throws Exception{
		String retval = DEFAULT_URI_PREFIX + localFragment;
		Boolean enumDetected = false;
		
		if(localFragment != "" && localFragment != null && !localFragment.isEmpty()){
			// check if fragment is from an enum
			if(this.oInfo != null){
				if(oInfo.classIsEnumeration(classUri)){
					enumDetected = true;
				}
				String val = oInfo.getMatchingEnumeration(classUri, localFragment);
				if(enumDetected  && val != null){
					retval = val;
				}
				else{
					// the requested class was an enumerated type but the value passed was not a valid 
					// instance of that enumeration.
					if(enumDetected && val == null ){
						throw new Exception("the class '" + classUri + "' is an enumeration but the value '" + localFragment + "' is not a valid member of the enumeration.");
					}
				}
				
			}
			// if it is not an enum, check for a user prefix
			if(!enumDetected && this.userUriPrefix != null && this.userUriPrefix != "" && !userUriPrefix.isEmpty()){
				if(!localFragment.contains("#")){  // make sure that the passed value does not have a prefix of its own.
					retval = this.userUriPrefix + localFragment;}
			}
			if(!enumDetected && localFragment.contains("#")){
				// this is not enumerated and contains a prefix, so it is likely a URI already. just pass it on.
				retval = localFragment;
			}
		}
		
		else{  // localfragment was blank
			
			retval = localFragment;
		}
		
		// return results
		return retval;
	}
	
	public String getInstanceUriWithPrefix(String classUri, String localFragment) throws Exception{
		String retval = DEFAULT_URI_PREFIX + localFragment;
		
		// empty
		if(localFragment == null  || localFragment.isEmpty()) {
			retval = "";
			
		// fragment is from an enum
		} else if (this.oInfo != null && oInfo.classIsEnumeration(classUri)) {
			retval = oInfo.getMatchingEnumeration(classUri, localFragment);
			if( retval == null){
				throw new Exception("the class '" + classUri + "' is an enumeration but the value '" + localFragment + "' is not a valid member of the enumeration.");
			}

		// if it is not an enum
		} else {
			
			// already contains #
			if(localFragment.contains("#")){
				retval = localFragment;
			
			// else prepend the prefix if there is one
			} else if(this.userUriPrefix != null && !userUriPrefix.isEmpty() && !localFragment.contains("#")){  
				retval = this.userUriPrefix + localFragment;
			}
		}

		// return results
		return retval;
	}

}
