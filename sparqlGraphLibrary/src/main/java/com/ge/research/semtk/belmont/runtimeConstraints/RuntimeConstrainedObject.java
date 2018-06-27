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

import java.util.ArrayList;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraints.SupportedTypes;
import com.ge.research.semtk.utility.LocalLogger;

public class RuntimeConstrainedObject{
	// this is used to store Nodes, PropertyItems which might be runtime constrained. 
	// it has to work flexibly with all and any of the above. 		
	
	
	private RuntimeConstraints.SupportedTypes objectType;
	private Returnable constrainedObject; 
	private SupportedOperations operation;
	private ArrayList<String> operands;
	
	public RuntimeConstrainedObject(Returnable obj, RuntimeConstraints.SupportedTypes objType){
		this.objectType = objType;
		this.constrainedObject = obj;
	}
	
	public String getObjectName(){
		String retval = this.constrainedObject.getRuntimeConstraintID();
		return retval;
	}
	
	public void applyConstraint(SupportedOperations operation, ArrayList<String> operands) throws Exception{
		this.operation = operation;
		this.operands = operands;
		
		XSDSupportedType xsdType = this.constrainedObject.getValueType();
		String sparqlId = this.constrainedObject.getSparqlID();
		
				
		if(operation == SupportedOperations.GREATERTHAN && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
			
			this.setNumberGreaterThan(sparqlId, val, false);
		}
		else if(operation == SupportedOperations.GREATERTHAN && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
			
			this.setDateAfter(sparqlId, val, false);
		}
		else if(operation == SupportedOperations.GREATERTHANOREQUALS && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberGreaterThan(sparqlId, val, true);
		}
		else if(operation == SupportedOperations.GREATERTHANOREQUALS && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateAfter(sparqlId, val, true);
		}
		else if(operation == SupportedOperations.LESSTHAN && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberLessThan(sparqlId, val, false);
		}
		else if(operation == SupportedOperations.LESSTHAN && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateBefore(sparqlId, val, false);
		}
		else if(operation == SupportedOperations.LESSTHANOREQUALS && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberLessThan(sparqlId, val, true);
		}
		else if(operation == SupportedOperations.LESSTHANOREQUALS && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateBefore(sparqlId, val, true);
		}
		else if(operation == SupportedOperations.MATCHES && xsdType.dateOperationAvailable()){
			// create a constraint to match the provided
			this.setDateMatchesConstraint(sparqlId, operands);
		
		}
		else if(operation == SupportedOperations.MATCHES){
			// create a constraint to match the provided
			this.setMatchesConstraint(sparqlId, operands);
		
		}
		else if(operation == SupportedOperations.REGEX){
			// create a regex entry
			this.setRegexConstraint(sparqlId, operands.get(0));
		
		}
		else if(operation == SupportedOperations.VALUEBETWEEN && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double valLow  = Double.parseDouble(operands.get(0));
			Double valHigh = Double.parseDouble(operands.get(1));
			
			this.setNumberInInterval(sparqlId, valLow, valHigh, true, true);
		
		}
		else if(operation == SupportedOperations.VALUEBETWEEN && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String valLow  = operands.get(0);
			String valHigh = operands.get(1);
			
			this.setDateInInterval(sparqlId, valLow, valHigh, true, true);
		
		}
		else if(operation == SupportedOperations.VALUEBETWEENUNINCLUSIVE && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double valLow  = Double.parseDouble(operands.get(0));
			Double valHigh = Double.parseDouble(operands.get(1));
						
			this.setNumberInInterval(sparqlId, valLow, valHigh, false, false);			
		}
		else if(operation == SupportedOperations.VALUEBETWEENUNINCLUSIVE && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String valLow  = operands.get(0);
			String valHigh = operands.get(1);
						
			this.setDateInInterval(sparqlId, valLow, valHigh, false, false);			
		}
		else{
			throw new Exception("RuntimConstrainedItems.selectAndSetConstraint :: Operation " + operation.name() + " has no mapped operations.");
		}
				
	}
	
	public String getOperationName(){ return this.operation.name(); }
	public ArrayList<String> getOperands(){ return this.operands; }

	public ValueConstraint getValueConstraint(){
		
		// create the VC and return it.	
		return this.constrainedObject.getValueConstraint();
	}
	
	public XSDSupportedType getValueType(){
		return this.constrainedObject.getValueType();
	}
	
	public SupportedTypes getObjectType(){
		return this.objectType;
	}
	
	private void setValueConstraint(String vcString){
		this.constrainedObject.setValueConstraint(new ValueConstraint(vcString));
	}
	
	//_____________________end runtime constraint applications.
	
	// methods that are most likely in the first rev to be used to fill in constraints. 
	// matching constraints.
	private void setMatchesConstraint(String sparqlId, ArrayList<String> inputs) throws Exception{
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getMatchesOneOfConstraint(sparqlId, inputs, getType(sparqlId));
		LocalLogger.logToStdOut("======= setMatchesConstraint for " + sparqlId + " to " + constraintStr);
		this.setValueConstraint(constraintStr);
	}

	private void setDateMatchesConstraint(String sparqlId, ArrayList<String> inputs) throws Exception{
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getDateMatchesOneOfConstraint(sparqlId, inputs, getType(sparqlId));
		this.setValueConstraint(constraintStr);
	}
	
	// regex
	private void setRegexConstraint(String sparqlId, String regexFragment) throws Exception{
		String constraintStr = ConstraintUtil.getRegexConstraint(sparqlId, regexFragment, getType(sparqlId));
		this.setValueConstraint(constraintStr);
	}
	
	// intervals.
	private void setNumberInInterval(String sparqlId, Double lowerBound, Double upperBound, Boolean greaterThanOrEqualToLower, Boolean lessThanOrEqualToUpper) throws Exception{
		String constraintStr = ConstraintUtil.getNumberBetweenConstraint(sparqlId, lowerBound.toString(), upperBound.toString(), getType(sparqlId), greaterThanOrEqualToLower, lessThanOrEqualToUpper);
		this.setValueConstraint(constraintStr);
	}
	
	private void setDateInInterval(String sparqlId, String lowerBound, String upperBound, Boolean greaterThanOrEqualToLower, Boolean lessThanOrEqualToUpper) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodBetweenConstraint(sparqlId, lowerBound, upperBound, getType(sparqlId), greaterThanOrEqualToLower, lessThanOrEqualToUpper);
		this.setValueConstraint(constraintStr);
	}
	
	
	// greater or less than.
	private void setNumberLessThan(String sparqlId, Double upperBound, Boolean lessThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getLessThanConstraint(sparqlId, upperBound.toString(), getType(sparqlId), lessThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	private void setDateBefore(String sparqlId, String upperBound, Boolean lessThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodBeforeConstraint(sparqlId, upperBound, getType(sparqlId), lessThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	private void setNumberGreaterThan(String sparqlId, Double lowerBound, Boolean greaterThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getGreaterThanConstraint(sparqlId, lowerBound.toString(), getType(sparqlId), greaterThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	private void setDateAfter(String sparqlId, String lowerBound, Boolean greaterThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodAfterConstraint(sparqlId, lowerBound, getType(sparqlId), greaterThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	
	// get the value type of the constraint based on the sparqlId
	private XSDSupportedType getType(String sparqlId) throws Exception{
		
		return this.constrainedObject.getValueType();
	}
	

}

