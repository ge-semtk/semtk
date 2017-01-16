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


package com.ge.research.semtk.logging;

import java.util.ArrayList;
import java.util.Objects;

public class DetailsTuple {

	private String detailName;
	private String detailValue;
	
	public DetailsTuple(String name, String dt){
		
		this.detailName = name;
		if (dt == null){ this.detailValue = ""; }
		
		else {
			this.detailValue = dt.replace("\"", "\\\"");   // PEC 
		}
		
		// PEC: I don't think this was actually needed.  
		// Now that we're not using this for a unique id any more, we don't need to wreck the string
		
		/*
		else {
			this.detailValue = dt.replaceAll("\n", " ");
			this.detailValue = this.detailValue.replace("\"","&QUOTE "); 
			this.detailValue = this.detailValue.replace("'","&SQUOTE "); 
			this.detailValue = this.detailValue.replace("\n", " ");
			this.detailValue = this.detailValue.replace("\r", " ");
		}
		*/
	}
	
	public String getValue(){
		return this.detailValue;
	}
	
	public String getName(){
		return this.detailName;
	}
	// create a collection of DetailTuples from a name and a collection of Detail values. 
	public static ArrayList<DetailsTuple> createGroupFromInputArray(String Name, ArrayList<String> values, ArrayList<DetailsTuple> retval){
		// use the values in the list to 
		if(retval == null){ retval = new ArrayList<DetailsTuple>(); }
		// populate the actual DetailsTuple
		for(String i : values){
			// create a new one
			retval.add(new DetailsTuple(Name, i));
		}
		// return the arrayList
		return retval;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DetailsTuple that = (DetailsTuple) o;
		return Objects.equals(detailName, that.detailName) &&
				Objects.equals(detailValue, that.detailValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(detailName, detailValue);
	}
}
