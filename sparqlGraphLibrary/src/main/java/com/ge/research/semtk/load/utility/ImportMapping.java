package com.ge.research.semtk.load.utility;

import java.util.ArrayList;

public class ImportMapping {
	private int sNodeIndex = -1;
	private int propItemIndex = -1;
	private ArrayList<MappingItem> itemList = new ArrayList<MappingItem>();
	private boolean isEnum = false;     // does sNodeIndex point to an enum
	
	public int getsNodeIndex() {
		return sNodeIndex;
	}
	public void setsNodeIndex(int sNodeIndex) {
		this.sNodeIndex = sNodeIndex;
	}
	public int getPropItemIndex() {
		return propItemIndex;
	}
	public void setPropItemIndex(int propItemIndex) {
		this.propItemIndex = propItemIndex;
	}
	public void setIsEnum(boolean b) {
		this.isEnum = b;
	}
	public boolean getIsEnum() {
		return this.isEnum;
	}
	public boolean isProperty() {
		return this.propItemIndex > -1;
	}
	
	public boolean isNode() {
		return this.propItemIndex == -1;
	}
	
	public void addItem(MappingItem item) {
		this.itemList.add(item);
	}
	
	/**
	 * Build the string for a node or property import value
	 * @param record
	 * @return
	 * @throws Exception
	 */
	public String buildString(ArrayList<String> record) throws Exception {
		StringBuilder ret = new StringBuilder();
		
		String str = "";
		MappingItem item;

		for (int i=0; i < this.itemList.size(); i++) {
			item = this.itemList.get(i);
			str = item.buildString(record);
			
			// URI may not have empty columns in it's build unless it is an enum
			// enum will either:
			//    - be totally empty and prune, or 
			//    - evaluate to a valid or invalid value and be treated accordingly
			if (str.equals("") && this.propItemIndex == -1 && ! this.isEnum && item.isColumnMapping() ) {
				throw new Exception("Empty values in URI build");
			}
			
			// build the return
			ret.append(str);
		}
		
		return ret.toString();
	}
}
