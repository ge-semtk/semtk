/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.querygen;

public abstract class EdcConstraintValue {
	
	protected static String[] allowedTypes;
	protected String val;
	protected String type;
	
	public EdcConstraintValue(String value, String type) throws Exception{
		setAllowedTypes();  // have the subclass set the allowed types 
		
		this.val  = value;
		
		Boolean validType = false;											// check that this type is actually one we know of. 
		for(String s : allowedTypes){	
			if(type.equalsIgnoreCase(s)){
				validType = true;											// found it. 
				break;
			}
		}
		if(validType){
			this.type = type.toUpperCase();
		}
		else{
			throw new Exception("an invalid type was given for a ConstraintValue : " + type + " is unknown");
		}
	}

	public abstract String getValue();
	public abstract String getType();
	public abstract void setAllowedTypes();
}
