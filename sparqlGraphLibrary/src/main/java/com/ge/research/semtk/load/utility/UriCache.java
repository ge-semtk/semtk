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
	static final String DUPLICATE = "0-DUPLICATE";
		
	ConcurrentHashMap<String, String> uriCache = new ConcurrentHashMap<String, String>();   // uriCache(key)=uri
	ConcurrentHashMap<String, Boolean> notFound = new ConcurrentHashMap<String, Boolean>(); // any == NOT_FOUND
	ConcurrentHashMap<String, Boolean> isGenerated = new ConcurrentHashMap<String, Boolean>();  // any URI that was generated
	
	/**
	 * This object holds URI's during the lookup process.
	 * The unique "key" is the node type and mappedStrings. 
	 * Keeps track of whether each is found or not.
	 * Generates URI's for the not founds.
	 * ...
	 * 
	 * @param nodeGroup
	 */
	public UriCache() {
	}
	
	/**
	 * 
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param mappedStrings
	 * @return
	 */
	private String getKey(String lookupNgMD5, ArrayList<String> mappedStrings) {
		if (mappedStrings.size() == 1) {
			// most common most efficient
			return mappedStrings.get(0) + "-" + lookupNgMD5;
		} else {
			return String.join("-", mappedStrings) + "-" + lookupNgMD5;
		}
	}
	
	/**
	 * Assign a URI given a unique lookup nodegroup and set of mappedStrings
	 * If it already exists and URI is the same:  do nothing
	 * If it already exists and URI is different:  change uri to DUPLICATE
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param mappedStrings
	 * @param uri
	 */
	public void putUri(String lookupNgMD5, ArrayList<String> mappedStrings, String uri) {
		String prev = this.uriCache.putIfAbsent(this.getKey(lookupNgMD5, mappedStrings), uri);
		if (prev != null && !prev.equals(uri)) {
			this.uriCache.put(this.getKey(lookupNgMD5, mappedStrings), DUPLICATE);
		}
	}
	
	/**
	 * Get a URI given a unique lookup nodegroup and set of mappedStrings
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param mappedStrings
	 * @return  URI, DUPLICATE, or NOT_FOUND
	 */
	public String getUri(String lookupNgMD5, ArrayList<String> mappedStrings) {
		try {
			//LocalLogger.logToStdErr("GET  : " + this.getKey(lookupNgMD5, mappedStrings));
			return this.uriCache.get(this.getKey(lookupNgMD5, mappedStrings));
		} catch (Exception e) {
			//LocalLogger.logToStdErr("...not found");
			return null;
		}
	}
	
	/**
	 * Is the given URI in the cache and not generated, then it must have been looked up
	 * The "this.uriCache.contains(uri)" can be very expensive.  Think twice.
	 * @param uri
	 * @return
	 */
	public boolean wasFound(String uri) {
		return this.uriCache.contains(uri) && ! this.isGenerated(uri);
	}
	
	/**
	 * Declare a URI 'not found', possibly suggesting a new value
	 * @param lookupNgMD5 - unique hash of the lookup nodegroup
	 * @param mappedStrings
	 * @param generatedValue - value for URI ... or null or ""
	 */
	public void setUriNotFound(String lookupNgMD5, ArrayList<String> mappedStrings, String generatedValue) throws Exception {
		String key = this.getKey(lookupNgMD5, mappedStrings);
		String newVal;
		
		if (generatedValue != null && generatedValue.length() > 0) {
			newVal = generatedValue;
			this.isGenerated.put(newVal, true);
		} else {
			newVal = UriCache.NOT_FOUND;
		}
		
		String prevVal = this.uriCache.putIfAbsent(key, newVal);
		if (prevVal != null && !prevVal.equals(newVal)) {
			throw new Exception("Can't create a URI with two different values: " + prevVal + " and " + newVal);
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
