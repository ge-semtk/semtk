package com.ge.research.semtk.load.utility;

import java.util.ArrayList;

public class ImportMapping {
	private int sNodeIndex = -1;
	private int propItemIndex = -1;
	private ArrayList<MappingItem> itemList = new ArrayList<MappingItem>();
	
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
	
	public boolean isProperty() {
		return propItemIndex > -1;
	}
	
	public void addItem(MappingItem item) {
		this.itemList.add(item);
	}
	
	public String buildString(ArrayList<String> record) {
		StringBuilder ret = new StringBuilder();
		for (int i=0; i < this.itemList.size(); i++) {
			ret.append(this.itemList.get(i).buildString(record));
		}
		return ret.toString();
	}
}
