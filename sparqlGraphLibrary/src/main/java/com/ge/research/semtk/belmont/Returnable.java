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

public abstract class Returnable {
	
	protected String sparqlID = null;
	protected Boolean isReturned = false;
	protected Boolean isRuntimeConstrained = false;
	
	// the constraints which will be applied on qry
	protected ValueConstraint constraints = null;
	
	/**
	 * 
	 * @return sparqlID or "" (not null)
	 */
	public String getSparqlID() {
		return this.sparqlID != null ? this.sparqlID : "";
	}

	public void setSparqlID(String iD) {
		this.sparqlID = iD;		
	}
	
	public boolean getIsReturned() {
		return this.isReturned;
	}
	public void setIsReturned(Boolean ret) {
		this.isReturned = ret;
	}
	
	public String getRuntimeConstraintID(){
		return this.getSparqlID();
	}

	public boolean getIsRuntimeConstrained(){
		return this.isRuntimeConstrained;
	}
	
	public void setIsRuntimeConstrained(boolean constrained){
		this.isRuntimeConstrained = constrained;
	}
	
	public void setValueConstraint(ValueConstraint v) {
		this.constraints = v;
	}
	public ValueConstraint getValueConstraint(){
		return this.constraints;
	}
	
	public abstract XSDSupportedType getValueType();
	

	
}
