package com.ge.research.semtk.belmont.runtimeConstraints;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;

public class RuntimeConstrainedItems {

	// date formats are supposed to look like this: 2014-05-23T10:20:13
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
	
	public static enum SupportedTypes { NODE , PROPERTYITEM };
	
	private NodeGroup parentNodeGroup;
	private HashMap<String, RuntimeConstrainedObject> members;
	
	public RuntimeConstrainedItems(NodeGroup parent){
		this.parentNodeGroup = parent;
		
		// set up the constraint items
		this.members = parent.getConstrainedItems();
		
	}
	
	public void addConstrainedItem(Returnable rt, SupportedTypes type){
		
		RuntimeConstrainedObject curr = new RuntimeConstrainedObject(rt, type);
		this.members.put(rt.getSparqlID(), curr);
	}
	
	
	public ArrayList<String> getConstrainedItemIds(){
		ArrayList<String> retval = new ArrayList<String>();
		
		for(String s : this.members.keySet() ){
			retval.add(s);
		}
		
		return retval;
	}
	
	// methods that are most likely in the first rev to be used to fill in constraints. 
	// matching constraints.
	public void setMatchesConstraint(String sparqlId, int[] vals) throws Exception{
		ArrayList<String> inputs = new ArrayList<String>();
		// convert the incoming array to what we need. 
		for(int dt : vals){
			inputs.add( dateFormat.format(dt));
		}
		
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getMatchesOneOfConstraint(sparqlId, inputs, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	public void setMatchesConstraint(String sparqlId, double[] vals) throws Exception{
		ArrayList<String> inputs = new ArrayList<String>();
		// convert the incoming array to what we need. 
		for(double dt : vals){
			inputs.add( dateFormat.format(dt));
		}
		
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getMatchesOneOfConstraint(sparqlId, inputs, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	public void setMatchesConstraint(String sparqlId, String[] vals) throws Exception{
		ArrayList<String> inputs = new ArrayList<String>();
		// convert the incoming array to what we need. 
		for(String dt : vals){
			inputs.add( dt);
		}
		
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getMatchesOneOfConstraint(sparqlId, inputs, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	public void setMatchesConstraint(String sparqlId, Date[] vals)  throws Exception{
		ArrayList<String> inputs = new ArrayList<String>();
		// convert the incoming array to what we need. 
		for(Date dt : vals){
			inputs.add( dateFormat.format(dt));
		}
		
		// create the constraint string. 
		String constraintStr = ConstraintUtil.getMatchesOneOfConstraint(sparqlId, inputs, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);		
	}
	public void setMatchesConstraint(String sparqlId, int val) throws Exception{
		String constraintStr = ConstraintUtil.getMatchesConstraint(sparqlId, ((Integer)val).toString(), getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	public void setMatchesConstraint(String sparqlId, double val) throws Exception{
		String constraintStr = ConstraintUtil.getMatchesConstraint(sparqlId, ((Double)val).toString(), getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	public void setMatchesConstraint(String sparqlId, String val) throws Exception{
		String constraintStr = ConstraintUtil.getMatchesConstraint(sparqlId, val, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	public void setMatchesConstraint(String sparqlId, Date val) throws Exception{
		String constraintStr = ConstraintUtil.getMatchesConstraint(sparqlId, dateFormat.format(val), getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	// regex
	public void setRegexConstraint(String sparqlId, String regexFragment) throws Exception{
		String constraintStr = ConstraintUtil.getRegexConstraint(sparqlId, regexFragment, getTypeName(sparqlId));
		this.setValueContraint(sparqlId, constraintStr);
	}
	
	// intervals.
	
	
	// get the value type of the constraint based on the sparqlId
	private String getTypeName(String sparqlId) throws Exception{
		RuntimeConstrainedObject rco = this.members.get(sparqlId);
		return rco.getValueType();
	}
	
	
	public ValueConstraint getValueConstraint(String itemSparqlId){
		ValueConstraint retval = null;
		
		// check to see if this item is in our list.
		if(members.containsKey(itemSparqlId)){
			retval = members.get(itemSparqlId).getValueConstraint();
		}
		
		return retval;
	}
	
	public String getValueType(String itemSparqlId) throws Exception{
		String retval = null;
		
		// check to see if this item is in our list.
		if(members.containsKey(itemSparqlId)){
			retval = members.get(itemSparqlId).getValueType();
		}
		else{
			throw new Exception(itemSparqlId + " does not exist in the available runtime constrained items.");
		}
		
		return retval;
	}
	
	public void setValueContraint(String itemSparqlId, String constraint) throws Exception{
		
		// check to see if this item is in our list.
		if(members.containsKey(itemSparqlId)){
			members.get(itemSparqlId).setConstraint(constraint);
		}
		else{
			throw new Exception("Constraint setting failed for SparqlId (" + itemSparqlId +
					") because it does not exist in the available runtime constrained items.");
		}
	}
	
	public String getItemType(String itemSparqlId) throws Exception{
		String retval = "";
		
		if(members.containsKey(itemSparqlId)){
			SupportedTypes st = members.get(itemSparqlId).getObjectType();

			retval = st.name();		// get the name.
		}
		else{
			throw new Exception(itemSparqlId + " does not exist in the available runtime constrained items.");
		}
		return retval;
	}
}
