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

package com.ge.research.semtk.belmont.runtimeConstraints;

import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstrainedItems.SupportedTypes;

public class RuntimeConstrainedObject{
	// this is used to store Nodes, PropertyItems which might be runtime constrained. 
	// it has to work flexibly with all and any of the above. 		
	
	
	private RuntimeConstrainedItems.SupportedTypes objectType;
	private Returnable constrainedObject; 
	
	public RuntimeConstrainedObject(Returnable obj, RuntimeConstrainedItems.SupportedTypes objType){
		this.objectType = objType;
		this.constrainedObject = obj;
	}
	
	public String getObjectName(){
		String retval = this.constrainedObject.getRuntimeConstraintID();
		return retval;
	}
	
	public void setConstraint(String constraint){
		this.constrainedObject.setValueConstraint(new ValueConstraint(constraint));
	}
	
	public void setConstraint(ValueConstraint vc){
		this.constrainedObject.setValueConstraint(vc);
	}
	
	public ValueConstraint getValueConstraint(){
		return this.constrainedObject.getValueConstraint();
	}
	
	public String getValueType(){
		String retval = this.constrainedObject.getValueType();
		return retval.toUpperCase(); // changes to upper case to match expected ID names in the "XSDSupportedTypes" enum. 
	}
	
	public SupportedTypes getObjectType(){
		return this.objectType;
	}
}

