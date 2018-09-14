/**
 ** Copyright 2016-2018 General Electric Company
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

import java.util.ArrayList;

import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager.SupportedTypes;
import com.ge.research.semtk.utility.LocalLogger;

public class RuntimeConstraintMetaData{
	// Meta data about operators, operands, and items involved in each constraint
	// and the translation code to apply this info to the nodegroup as a ValueConstraint
	
	
	private RuntimeConstraintManager.SupportedTypes itemType;
	private Returnable ngItem; 
	private SupportedOperations operation;
	

	private ArrayList<String> operands;
	
	public RuntimeConstraintMetaData(Returnable item, RuntimeConstraintManager.SupportedTypes itemType){
		this.itemType = itemType;
		this.ngItem = item;
	}
	
	public String getObjectName(){
		String retval = this.ngItem.getRuntimeConstraintID();
		return retval;
	}
	
	public boolean constraintIsApplied() {
		return this.operation != null;
	}
	
	public void applyConstraint(SupportedOperations operation, ArrayList<String> operands) throws Exception{
		this.operation = operation;
		this.operands = operands;
		
		String sparqlId = this.ngItem.getSparqlID();
		
		if(operation == SupportedOperations.GREATERTHAN ){
			this.setGreaterThan(sparqlId, operands.get(0), false);
		}
		
		else if(operation == SupportedOperations.GREATERTHANOREQUALS){
			this.setGreaterThan(sparqlId, operands.get(0), true);
		}
		
		else if(operation == SupportedOperations.LESSTHAN ){
			this.setLessThan(sparqlId, operands.get(0), false);
		}
		
		else if(operation == SupportedOperations.LESSTHANOREQUALS){
			this.setLessThan(sparqlId, operands.get(0), true);
		}
		
		else if(operation == SupportedOperations.MATCHES){
			this.setMatchesConstraint(sparqlId, operands);
		}
		
		else if(operation == SupportedOperations.REGEX){
			this.setRegexConstraint(sparqlId, operands.get(0));
		}
		
		else if(operation == SupportedOperations.VALUEBETWEEN){
			this.setRange(sparqlId, operands.get(0), operands.get(1), false, false);
		}
		
		else if(operation == SupportedOperations.VALUEBETWEENUNINCLUSIVE){
			this.setRange(sparqlId, operands.get(0), operands.get(1), true, true);
		}
		
		else{
			throw new Exception("RuntimConstrainedItems.selectAndSetConstraint :: Operation " + operation.name() + " has no mapped operations.");
		}
				
	}
	public SupportedOperations getOperation() {
		return operation;
	}
	public String getOperationName(){ return this.operation.name(); }
	public ArrayList<String> getOperands(){ return this.operands; }

	public ValueConstraint getValueConstraint(){
		
		// create the VC and return it.	
		return this.ngItem.getValueConstraint();
	}
	
	public XSDSupportedType getValueType(){
		return this.ngItem.getValueType();
	}
	
	public SupportedTypes getObjectType(){
		return this.itemType;
	}
	
	private void setValueConstraint(String vcString){
		this.ngItem.setValueConstraint(new ValueConstraint(vcString));
	}
	
	//_____________________end runtime constraint applications.
	
	// methods that are most likely in the first rev to be used to fill in constraints. 
	// matching constraints.
	private void setMatchesConstraint(String sparqlId, ArrayList<String> inputs) throws Exception{
		// create the constraint string. 
		String constraintStr = ValueConstraint.buildValuesConstraint(sparqlId, inputs, getType(sparqlId));
		LocalLogger.logToStdOut("======= setMatchesConstraint for " + sparqlId + " to " + constraintStr);
		this.setValueConstraint(constraintStr);
	}
	
	// regex
	private void setRegexConstraint(String sparqlId, String regexFragment) throws Exception{
		String constraintStr = ValueConstraint.buildRegexConstraint(sparqlId, regexFragment, getType(sparqlId));
		this.setValueConstraint(constraintStr);
	}
	
	// intervals.
	private void setRange(String sparqlId, String lowerBound, String upperBound, Boolean greaterThanOrEqualToLower, Boolean lessThanOrEqualToUpper) throws Exception{
		String constraintStr = ValueConstraint.buildRangeConstraint(sparqlId, lowerBound.toString(), upperBound.toString(), getType(sparqlId), greaterThanOrEqualToLower, lessThanOrEqualToUpper);
		this.setValueConstraint(constraintStr);
	}
	
	
	// greater or less than.
	private void setLessThan(String sparqlId, String upperBound, Boolean lessThanOrEqualTo) throws Exception{
		String constraintStr = ValueConstraint.buildLessThanConstraint(sparqlId, upperBound.toString(), getType(sparqlId), lessThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	private void setGreaterThan(String sparqlId, String lowerBound, Boolean greaterThanOrEqualTo) throws Exception{
		String constraintStr = ValueConstraint.buildGreaterThanConstraint(sparqlId, lowerBound, getType(sparqlId), greaterThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	// get the value type of the constraint based on the sparqlId
	private XSDSupportedType getType(String sparqlId) throws Exception{
		
		return this.ngItem.getValueType();
	}
	
	
}

