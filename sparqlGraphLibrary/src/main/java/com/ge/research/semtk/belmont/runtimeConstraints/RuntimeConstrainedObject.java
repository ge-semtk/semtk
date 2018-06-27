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
	
	
	private RuntimeConstraints.SupportedTypes itemType;
	private Returnable ngItem; 
	private SupportedOperations operation;
	private ArrayList<String> operands;
	
	public RuntimeConstrainedObject(Returnable item, RuntimeConstraints.SupportedTypes itemType){
		this.itemType = itemType;
		this.ngItem = item;
	}
	
	public String getObjectName(){
		String retval = this.ngItem.getRuntimeConstraintID();
		return retval;
	}
	
	public void applyConstraint(SupportedOperations operation, ArrayList<String> operands) throws Exception{
		this.operation = operation;
		this.operands = operands;
		
		this.selectAndSetConstraint(this.ngItem.getSparqlID(), operation.name(), this.ngItem.getValueType(), operands);
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
	
	
	// find and create the appropriate constraint
	public void selectAndSetConstraint(String sparqlId, String operationID, XSDSupportedType xsdType, ArrayList<String> operands) throws Exception{
		// a big switch statement to figure out which operation, if any we want. 
		
		// check that operator is sane.
		try{
			SupportedOperations.valueOf(operationID.toUpperCase());
		}
		catch(Exception e){
			String supported = "";
			int supportCount = 0;
			for (SupportedOperations c :  SupportedOperations.values()){
				if(supportCount != 0){ supported += " or "; }
				supported += c.name();
				supportCount++;
			}	
			throw new Exception("RuntimConstrainedItems.selectAndSetConstraint :: Operation " + operationID + " not recognized. Recognized operations are: " + supported);
		}
		
		// inputs are sane (sparqlId and operands checked earlier)
		// a large switch statement of our options with a catch all for misunderstood options at the bottom. 
		
		if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHAN.name()) && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
			
			this.setNumberGreaterThan(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHAN.name()) && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
			
			this.setDateAfter(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHANOREQUALS.name()) && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberGreaterThan(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.GREATERTHANOREQUALS.name()) && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateAfter(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHAN.name()) && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberLessThan(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHAN.name()) && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateBefore(sparqlId, val, false);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHANOREQUALS.name()) && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double val = Double.parseDouble(operands.get(0));
						
			this.setNumberLessThan(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.LESSTHANOREQUALS.name()) && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String val = operands.get(0);
						
			this.setDateBefore(sparqlId, val, true);
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.MATCHES.name()) && xsdType.dateOperationAvailable()){
			// create a constraint to match the provided
			this.setDateMatchesConstraint(sparqlId, operands);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.MATCHES.name())){
			// create a constraint to match the provided
			this.setMatchesConstraint(sparqlId, operands);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.REGEX.name())){
			// create a regex entry
			this.setRegexConstraint(sparqlId, operands.get(0));
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEEN.name()) && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double valLow  = Double.parseDouble(operands.get(0));
			Double valHigh = Double.parseDouble(operands.get(1));
			
			this.setNumberInInterval(sparqlId, valLow, valHigh, true, true);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEEN.name()) && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String valLow  = operands.get(0);
			String valHigh = operands.get(1);
			
			this.setDateInInterval(sparqlId, valLow, valHigh, true, true);
		
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEENUNINCLUSIVE.name()) && xsdType.numericOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			Double valLow  = Double.parseDouble(operands.get(0));
			Double valHigh = Double.parseDouble(operands.get(1));
						
			this.setNumberInInterval(sparqlId, valLow, valHigh, false, false);			
		}
		else if(operationID.toUpperCase().equals(SupportedOperations.VALUEBETWEENUNINCLUSIVE.name()) && xsdType.dateOperationAvailable()){
			// this only handles numeric types right now. dates will likely break.
			String valLow  = operands.get(0);
			String valHigh = operands.get(1);
						
			this.setDateInInterval(sparqlId, valLow, valHigh, false, false);			
		}
		else{
			throw new Exception("RuntimConstrainedItems.selectAndSetConstraint :: Operation " + operationID + " has no mapped operations. this is likely an oversite.");
		}
		
	
	}
	//_____________________end runtime constraint applications.
	
	// methods that are most likely in the first rev to be used to fill in constraints. 
	// matching constraints.
	public void setMatchesConstraint(String sparqlId, ArrayList<String> inputs) throws Exception{
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getMatchesOneOfConstraint(sparqlId, inputs, getType(sparqlId));
		LocalLogger.logToStdOut("======= setMatchesConstraint for " + sparqlId + " to " + constraintStr);
		this.setValueConstraint(constraintStr);
	}

	public void setDateMatchesConstraint(String sparqlId, ArrayList<String> inputs) throws Exception{
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getDateMatchesOneOfConstraint(sparqlId, inputs, getType(sparqlId));
		this.setValueConstraint(constraintStr);
	}
	
	// regex
	public void setRegexConstraint(String sparqlId, String regexFragment) throws Exception{
		String constraintStr = ConstraintUtil.getRegexConstraint(sparqlId, regexFragment, getType(sparqlId));
		this.setValueConstraint(constraintStr);
	}
	
	// intervals.
	public void setNumberInInterval(String sparqlId, Double lowerBound, Double upperBound, Boolean greaterThanOrEqualToLower, Boolean lessThanOrEqualToUpper) throws Exception{
		String constraintStr = ConstraintUtil.getNumberBetweenConstraint(sparqlId, lowerBound.toString(), upperBound.toString(), getType(sparqlId), greaterThanOrEqualToLower, lessThanOrEqualToUpper);
		this.setValueConstraint(constraintStr);
	}
	
	public void setDateInInterval(String sparqlId, String lowerBound, String upperBound, Boolean greaterThanOrEqualToLower, Boolean lessThanOrEqualToUpper) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodBetweenConstraint(sparqlId, lowerBound, upperBound, getType(sparqlId), greaterThanOrEqualToLower, lessThanOrEqualToUpper);
		this.setValueConstraint(constraintStr);
	}
	
	
	// greater or less than.
	public void setNumberLessThan(String sparqlId, Double upperBound, Boolean lessThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getLessThanConstraint(sparqlId, upperBound.toString(), getType(sparqlId), lessThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	public void setDateBefore(String sparqlId, String upperBound, Boolean lessThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodBeforeConstraint(sparqlId, upperBound, getType(sparqlId), lessThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	public void setNumberGreaterThan(String sparqlId, Double lowerBound, Boolean greaterThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getGreaterThanConstraint(sparqlId, lowerBound.toString(), getType(sparqlId), greaterThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	public void setDateAfter(String sparqlId, String lowerBound, Boolean greaterThanOrEqualTo) throws Exception{
		String constraintStr = ConstraintUtil.getTimePeriodAfterConstraint(sparqlId, lowerBound, getType(sparqlId), greaterThanOrEqualTo);
		this.setValueConstraint(constraintStr);
	}
	
	
	// get the value type of the constraint based on the sparqlId
	private XSDSupportedType getType(String sparqlId) throws Exception{
		
		return this.ngItem.getValueType();
	}
	

}

