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

import org.json.simple.JSONObject;

public abstract class Returnable {
	
	protected String sparqlID = null;
	protected Boolean isReturned = false;
	protected Boolean isTypeReturned = false;
	protected Boolean isRuntimeConstrained = false;
	protected String binding = null;
	protected Boolean isBindingReturned = false;
	
	// the constraints which will be applied on qry
	protected ValueConstraint constraints = null;
	
	protected void addReturnableJson(JSONObject ret) {
		ret.put("SparqlID", this.sparqlID);///
		ret.put("isReturned", this.isReturned);///
		ret.put("isRuntimeConstrained", this.getIsRuntimeConstrained());///
		ret.put("Constraints", this.constraints != null ? this.constraints.toString() : "");///valueConstraint

		if (this.isTypeReturned) {
			ret.put("isTypeReturned", true);
		}
		if (this.binding != null) {
			ret.put("binding", this.binding);
			ret.put("isBindingReturned", this.isBindingReturned);

		}
	}
	
	protected void fromReturnableJson(JSONObject jObj) {
		
		this.sparqlID = (String) jObj.get("SparqlID"); ///
		this.isReturned = (Boolean)jObj.get("isReturned");///

		if (jObj.containsKey("binding")) {
			this.setBinding((String) jObj.get("binding"));///
		} else {
			this.setBinding(null);
		}
		if (jObj.containsKey("isBindingReturned")) {
			this.setIsBindingReturned((Boolean)jObj.get("isBindingReturned"));///
		} else {
			this.setIsBindingReturned(false);///
		}
		
		if (jObj.containsKey("isTypeReturned")) {
			this.isTypeReturned = (Boolean)jObj.get("isTypeReturned"); ///
		} else {
			this.isTypeReturned = false;
		}
		
		try{
			this.setIsRuntimeConstrained((Boolean)jObj.get("isRuntimeConstrained"));  ///
		}
		catch(Exception E){
			this.setIsRuntimeConstrained(false);
		}
		
		String vc = null;
		if (jObj.containsKey("valueConstraint")) {
			vc = jObj.get("valueConstraint").toString();  
		} else if (jObj.containsKey("Constraints")) {
			// backwards compatible
			vc = jObj.get("Constraints").toString();  
		}
		
		if (vc != null && !vc.isEmpty()) {
			this.constraints = new ValueConstraint(vc);
		} else {  // change blank constraints to null
			this.constraints = null;
		} 
		
		
	}
	/**
	 * 
	 * @return sparqlID or "" (not null)
	 */
	public String getSparqlID() {
		return this.sparqlID != null ? this.sparqlID : "";
	}

	public void setSparqlID(String iD) {
		if (iD == null) {
			this.sparqlID = null;
		} else if (iD.length() > 0 && !iD.startsWith("?")) {
			this.sparqlID = "?" + iD;
		} else {
			this.sparqlID =iD;
		}
	}
	
	public boolean getIsReturned() {
		return this.isReturned;
	}

	public void setIsReturned(Boolean ret) {
		this.isReturned = ret;
	}
	
	public void setIsTypeReturned(boolean b){
		this.isTypeReturned = b;
	}
	
	public String getBinding() {
		return binding;
	}
	
	public String getBindingOrSparqlID() {
		if (this.binding != null && ! this.binding.isEmpty()) {
			return this.binding;
		} else {
			return this.getSparqlID();
		}
	}

	public void setBinding(String binding) {
		if (binding == null) {
			this.binding = null;
		} else {
			this.binding = binding.startsWith("?") ? binding : "?" + binding;
		}
	}

	// Is binding returned (it must also exist)
	public Boolean getIsBindingReturned() {
		return isBindingReturned && this.binding != null && !this.binding.isEmpty();
	}

	public void setIsBindingReturned(Boolean isBindingReturned) {
		this.isBindingReturned = isBindingReturned;
	}

	public String getTypeSparqlID() {
        return this.sparqlID + "_type";
    }
	
	public boolean getIsTypeReturned() {
		return this.isTypeReturned;
	}
	
	public String getRuntimeConstraintID(){
		return this.getSparqlID();
	}
	
	public boolean hasAnyReturn() {
		return this.isReturned || this.isTypeReturned || this.isBindingReturned;
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
