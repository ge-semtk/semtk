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

package com.ge.research.semtk.load.utility;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * One row of items mapping to a particular item in the nodegroup
 * @author 200001934
 *
 */
public class ImportMapping {
	public static String NO_PROPERTY = "";
    public static String TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	private String nodeSparqlID = null;
	private String propURI = NO_PROPERTY;   
	private ArrayList<MappingItem> itemList = new ArrayList<MappingItem>();
	private boolean isEnum = false;     // does sNodeIndex point to an enum

	public static ImportMapping importSpecCopy(ImportMapping other) {
		ImportMapping ret = new ImportMapping();
		ret.nodeSparqlID = other.nodeSparqlID;
		ret.propURI = other.propURI;
		for (MappingItem item : other.itemList) {
			ret.itemList.add(MappingItem.importSpecCopy(item)); 
		}
		ret.isEnum = other.isEnum;
		
		return ret;
	}
	
	public String getNodeSparqlID() {
		return nodeSparqlID;
	}


	public void setNodeSparqlID(String nodeSparqlID) {
		this.nodeSparqlID = nodeSparqlID;
	}


	public String getPropURI() {
		return propURI;
	}
	public void setPropURI(String propURI) {
		this.propURI = propURI;
	}
	public void setIsEnum(boolean b) {
		this.isEnum = b;
	}
	public boolean getIsEnum() {
		return this.isEnum;
	}
	public boolean isProperty() {
		return !this.propURI.equals(NO_PROPERTY) && !this.isTypeRestriction();
	}
    public boolean isTypeRestriction() {
        return this.propURI.equals(ImportMapping.TYPE_URI);
    }
    
	public boolean isNode() {
		return this.propURI.equals(NO_PROPERTY);
	}
	
	public void addItem(MappingItem item) {
		this.itemList.add(item);
	}
	
	public ArrayList<MappingItem> getItemList() {
		return this.itemList;
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
		int emptyCols = 0;
		int totalCols = 0;

		for (int i=0; i < this.itemList.size(); i++) {
			item = this.itemList.get(i);
			str = item.buildString(record);
			
			// keep running count of column mappings and how many are empty
			if (item.isColumnMapping()) {
				totalCols ++;
				
				if (str.equals("")) {
					emptyCols ++;
				}
			}
			
			// build result
			ret.append(str);
			
		}
		
		// Error checking for URI's (non-enum) with empty columns
		// (enums have further error-checking later in the pipeline)
		if (emptyCols > 0 && this.isNode() && ! this.getIsEnum()) {
			
			// URI with every column blank: make the value blank
			if (emptyCols == totalCols) {
				ret = new StringBuilder();
				
			// URI with some columns blank: throw an exception
			} else {
				throw new Exception("Empty values in some columns of a URI build");
			}
			
		}
		
		String retStr = ret.toString();
		
		// build the return
		if (false && ! Charset.forName("US-ASCII").newEncoder().canEncode(retStr)) {   // TODO remove "false &&" to re-enable this
			throw new Exception("Detected non-ascii character in input record: " + retStr);
		}
		return retStr;
	}

}
