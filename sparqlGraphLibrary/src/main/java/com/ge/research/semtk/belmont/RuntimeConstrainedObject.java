package com.ge.research.semtk.belmont;

import com.ge.research.semtk.belmont.RuntimeConstrainedItems.SupportedTypes;

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

