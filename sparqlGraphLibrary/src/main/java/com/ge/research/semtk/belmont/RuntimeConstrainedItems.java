package com.ge.research.semtk.belmont;

import java.util.ArrayList;
import java.util.HashMap;

public class RuntimeConstrainedItems {

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
