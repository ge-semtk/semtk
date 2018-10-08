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
package com.ge.research.semtk.auth;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * Library-independent representation of http headers
 * @author 200001934
 *
 */
public class HeaderTable {
	Hashtable<String,List<String>> tab = new Hashtable<String,List<String>>();	
	
	public void put(String key, List<String> vals) {
		this.tab.put(key,vals);
	}
	
	public List<String> get(String key) {
		return this.tab.get(key);
	}
	
	public Set<String> keySet() {
		return tab.keySet();
	}
	
}
