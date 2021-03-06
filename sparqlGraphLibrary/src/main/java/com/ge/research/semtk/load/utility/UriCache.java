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

package com.ge.research.semtk.load.utility;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.utility.LocalLogger;

public class UriCache {
	static final String NOT_FOUND = "0-NOT-FOUND";
	static final String EMPTY_LOOKUP = "1-EMPTY-LOOKUP";
	
	private NodeGroup nodeGroup;
	
	ConcurrentHashMap<String, String> uriCache = new ConcurrentHashMap<String, String>();   // uriCache(key)=uri
	ConcurrentHashMap<String, Boolean> notFound = new ConcurrentHashMap<String, Boolean>(); // any == NOT_FOUND
	ConcurrentHashMap<String, Boolean> isGenerated = new ConcurrentHashMap<String, Boolean>();  // any URI that was generated
	
	/**
	 * This object holds URI's during the lookup process.
	 * The unique "key" is the node type and builtStrings. 
	 * Keeps track of whether each is found or not.
	 * Generates URI's for the not founds.
	 * ...
	 * 
	 * @param nodeGroup
	 */
	public UriCache(NodeGroup nodeGroup) {
		this.nodeGroup = nodeGroup;
	}
	
	/**
	 * 
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param builtStrings
	 * @return
	 */
	private String getKey(String lookupNgMD5, ArrayList<String> builtStrings) {
		return lookupNgMD5 + "-" + String.join("-", builtStrings);
	}
	
	/**
	 * Assign a URI given a unique lookup nodegroup and set of builtStrings
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param builtStrings
	 * @param uri
	 */
	public void putUri(String lookupNgMD5, ArrayList<String> builtStrings, String uri) {
		this.uriCache.putIfAbsent(this.getKey(lookupNgMD5, builtStrings), uri);
		//LocalLogger.logToStdErr("PUT   : " + this.getKey(lookupNgMD5, builtStrings) + "," + uri);
	}
	
	/** 
	 * Add if URI isn't already there.  If it is already there, delete it instead.
	 * Intended to 
	 * @param lookupNgMD5
	 * @param builtStrings
	 * @param uri
	 */
	public void putIfNewElseDelete(String lookupNgMD5, ArrayList<String> builtStrings, String uri) {
		synchronized(this) {
			String key = this.getKey(lookupNgMD5, builtStrings);
		
			String prev = this.uriCache.putIfAbsent(key, uri);
			if (prev != null && !prev.equals(uri)) {
				this.uriCache.remove(key);
				//LocalLogger.logToStdErr("REMOVE: " + key + "," + uri);
				
			} else {
				//LocalLogger.logToStdErr("PUT-2 : " + key + "," + uri);
			}
		}
	}
	/**
	 * Get a URI given a unique lookup nodegroup and set of builtStrings
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param builtStrings
	 * @return
	 */
	public String getUri(String lookupNgMD5, ArrayList<String> builtStrings) {
		try {
			//LocalLogger.logToStdErr("GET  : " + this.getKey(lookupNgMD5, builtStrings));
			return this.uriCache.get(this.getKey(lookupNgMD5, builtStrings));
		} catch (Exception e) {
			//LocalLogger.logToStdErr("...not found");
			return null;
		}
	}
	
	/**
	 * Is it "notFound" given a unique lookup nodegroup and set of builtStrings
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param builtStrings
	 * @return
	 */
	public boolean isNotFound(String lookupNgMD5, ArrayList<String> builtStrings) {
		String key = this.getKey(lookupNgMD5, builtStrings);
		return this.notFound.containsKey(key);
	}
	
	/**
	 * Is the given URI in the cache and not generated, then it must have been looked up
	 * @param uri
	 * @return
	 */
	public boolean wasFound(String uri) {
		return this.uriCache.contains(uri) && ! this.isGenerated(uri);
	}
	
	/**
	 * Declare a URI 'not found', possibly suggesting a new value
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param builtStrings
	 * @param generatedValue - value for URI ... or null or ""
	 */
	public synchronized void setUriNotFound(String lookupNgMD5, ArrayList<String> builtStrings, String generatedValue) throws Exception {
		String key = this.getKey(lookupNgMD5, builtStrings);
		String newVal;
		
		if (generatedValue != null && generatedValue.length() > 0) {
			newVal = generatedValue;
			this.isGenerated.put(newVal, true);
		} else {
			newVal = UriCache.NOT_FOUND;
		}
		
		// If already NOT_FOUND
		if (this.uriCache.containsKey(key)) {
			// already there: do nothing as along as this value is the same
			String curVal = this.uriCache.get(key);
			if (!newVal.equals(curVal)) {
				throw new Exception("Can't create a URI with two different values: " + curVal + " and " + newVal);
			}
		} else {
			// new: just put it in the cache
			this.uriCache.put(key, newVal);
		}
		this.notFound.putIfAbsent(key, true);
	}
	
	/**
	 * Generate GUIDS for all this.notFound keys that were not already generated by a mapping
	 * Remove them from this.notFound
	 * Add them to this.isGenerated
	 * @param uriResolver
	 * @throws Exception
	 */
	public void generateNotFoundURIs(UriResolver uriResolver) throws Exception {
		for (String key : this.notFound.keySet()) {
			
			if (this.uriCache.get(key).equals(UriCache.NOT_FOUND)) {
				String guid = uriResolver.generateRandomUri();
				this.uriCache.put(key, guid);
				this.isGenerated.put(guid, true);
			}
			
		}
		// clear out notFound hash
		this.notFound = new ConcurrentHashMap<String, Boolean>();
	}
	
	/**
	 * was this Uri generated by this.generateNotFoundGuids
	 * @param uri
	 * @return
	 */
	public boolean isGenerated(String uri) {
		if (uri == null || uri.isEmpty()) {
			return false;
		} else {
			return this.isGenerated.containsKey(uri);
		}
	}
	
}
