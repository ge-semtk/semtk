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


package com.ge.research.semtk.belmont;


/**
 * Overrides to default query returns:  Only CONSTRUCT currently has any choice.
 */
public enum QueryReturnTypes {
	OWLRDF;
	
	/**
	 * More lenient lookup: adds QUERY_ and uppercases.
	 * (Couldn't figure out how to override valueOf()
	 * @param s
	 * @return
	 * @throws Exception
	 */
	public static QueryReturnTypes getMatchingValue(String s) throws Exception {
		String lookup = s.toUpperCase();
		return QueryReturnTypes.valueOf(lookup);
	}
}