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

public class ValueConstraint {

	private String constraint = "";
	
	public ValueConstraint(String vc){
		this.constraint = vc;
	}
	
	public String getConstraint(){
		return this.constraint;
	}
	
	public void changeSparqlID(String oldID, String newID) {
		if (this.constraint != null && oldID != null && newID != null) {
			this.constraint = this.constraint.replaceAll("\\" + oldID + "\\b", newID);
		}
	}
	/** 
	 * to non-null string
	 */
	public String toString() {
		return this.constraint == null ? "" : this.constraint;
	}
	
}
