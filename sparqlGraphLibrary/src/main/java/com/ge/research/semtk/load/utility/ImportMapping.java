package com.ge.research.semtk.load.utility;

import java.util.ArrayList;

public class ImportMapping {
	private int sNodeIndex = -1;
	private int propItemIndex = -1;
	private ArrayList<MappingItem> itemList = new ArrayList<MappingItem>();
	private boolean isEnum = false;     // does sNodeIndex point to an enum
	private int blanksInLastBuild = 0;  // how many blank columns contributed to last buildString()
	
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
	public int getBlanksInLastBuild() {
		return this.blanksInLastBuild;
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
	
	public String buildString(ArrayList<String> record) throws Exception {
		StringBuilder ret = new StringBuilder();
		
		this.blanksInLastBuild = 0;
		String str = "";
		MappingItem item;

		for (int i=0; i < this.itemList.size(); i++) {
			item = this.itemList.get(i);
			str = item.buildString(record);
			// count blank columns
			if (str.equals("") && item.isColumnMapping()) {
				this.blanksInLastBuild += 1;
			}
			// build the return
			ret.append(str);
		}
		
		return ret.toString();
	}
}
