/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.sparqlX.dispatch.QueryGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;

public class DispatchQueryGroup {

	private UUID guid;													// IDs this collection, based on the semantic (non-EDC) column values
	private HashMap<String, String> semanticColumnValues;				// the collection of "semantic" column values which define individuality
	private ArrayList<HashMap<String, String>> edcColumnValues; 		// the collection of sets of EDC-related values
	private ArrayList<Table> partialResults = new ArrayList<Table>();	// used to store the partial results from the service. 
																		// initialized to null for starters. it will be filled in when the results star to arrive.
	
	public DispatchQueryGroup(){
		this.guid = UUID.randomUUID();
		this.semanticColumnValues = new HashMap<String, String>();
		this.edcColumnValues = new ArrayList<HashMap<String,String>>();
	}
	
	public DispatchQueryGroup(UUID guid){
		this.guid = guid;
		this.semanticColumnValues = new HashMap<String, String>();
		this.edcColumnValues = new ArrayList<HashMap<String,String>>();
	}
	/**
	 * return the guid for this group
	 * @return
	 */
	public UUID getUUID(){
		return this.guid;
	}
	/**
	 * updates or adds a single value to the collection of semantic values
	 * @param colname
	 * @param value
	 */
	public void addSemanticColumnValue(String colname, String value){		// add/update a single value
		this.semanticColumnValues.put(colname, value);
	}
	/**
	 * replaces all of the semantic solumn values with a new hashmap
	 * @param scv
	 */
	public void addSemanticColumnValues(HashMap<String, String> scv){ 		
		this.semanticColumnValues = scv;
	}
	/**
	 * adds an entire EDC column set. if this set already exists, in its entirety, nothing happens.
	 * @param columnNames
	 * @param values
	 * @throws Exception
	 */
	public void addEdcColumnSet(String[] columnNames, String[] values) throws Exception{

		HashMap<String, String> newEntry = new HashMap<String, String>();
		if(columnNames == null || values == null ) { 
			throw new Exception("incomplete set sent to addEdcColumnSet");
		} // do not match the nonexistent. 
		else if(columnNames.length != values.length){ 
			throw new Exception("incomplete set sent to addEdcColumnSet");
		} // do not try if the sizes are off. 
		
		for(int i = 0; i < columnNames.length; i++){
			newEntry.put(columnNames[i], values[i]);
		}
		
		// add the new edc Value, if appropriate
		if(!this.edcMapAlreadyPresent(newEntry)){
			this.edcColumnValues.add(newEntry);
		}

	}
	/**
	 * adds an entire EDC set from a pre-prepared hash map.
	 * @param newEntry
	 */
	public void addEdcColumnSet(HashMap<String, String> newEntry){
	
		// add the new edc Value, if appropriate
		if(!this.edcMapAlreadyPresent(newEntry)){
			this.edcColumnValues.add(newEntry);
		}
	}
	/**
	 * takes in a hashMap of semantic values. if the collection given matches the internal set, return true. if not, return false.
	 * @param comparator
	 * @return
	 */
	public Boolean semanticMatchesInput(HashMap<String, String> comparator){
		Boolean retval = true;
		// check sizes and keys.
		if(comparator.keySet().size() != this.semanticColumnValues.keySet().size()) { retval = false; }
		else{
			// check the keys themselves
			for(String keyCurr : this.semanticColumnValues.keySet()){
				if(!comparator.keySet().contains(keyCurr)){
					// no need to continue, we do not have this value
					retval = false;
					break;
				}
				if(!comparator.get(keyCurr).equals(this.semanticColumnValues.get(keyCurr))){
					// at least one mismatched value, no need to continue
					retval = false;
					break;
				}
			}
		}
		// return our answer. 
		return retval;
	}
	/**
	 * takes in a pair of arrays which represent the set of semantic values. if the collection given matches the internal set, return true. if not, return false.
	 * @param columnNames
	 * @param values
	 * @return
	 */
	public Boolean semanticMatchesInput(String[] columnNames, String[] values){
		// note: it is assumed that the column names and values appear in the same order. 
		Boolean retval = true;
		
		if(columnNames == null || values == null ) { retval = false; } // do not match the nonexistent. 
		else if(columnNames.length != values.length){ retval = false;} // do not try if the sizes are off. 
		else{	// finally, a reason to try. 
			
			for (int counter = 0; counter < columnNames.length; counter++ ){
				if(!values[counter].equalsIgnoreCase(this.semanticColumnValues.get(columnNames[counter]))){
					// at least one did not match, no sense continuing. 
					retval = false;
					break;
				}
			}
			
		}
		
		return retval;
	}
	/**
	 * return the collection of EDC values 
	 * @return
	 */
	public ArrayList<HashMap<String, String>> getEdcColumnInfo(){
		return this.edcColumnValues;
	}
	/**
	 * return the set of semantic column info
	 * @return
	 */
	public HashMap<String, String> getSemanticColumnInfo(){
		return this.semanticColumnValues;
	}
	/**
	 * check to see if a given mp representing EDC values already exists.
	 * @param nv
	 * @return
	 */
	public Boolean edcMapAlreadyPresent(HashMap<String, String> nv){
		Boolean retval = false;
		
		Set<String> keys = nv.keySet();
		
		for(HashMap<String, String> curr : this.edcColumnValues){
			Boolean[] present = new Boolean[keys.size()];
			
			// quick back-of-envelope test: if the sizes do not match, these are not the same.
			if(keys.size() != curr.keySet().size()){
				continue;
			}
			
			// check for a match
			int i = 0;
			for(String k : keys){
				if(curr.get(k).equals(nv.get(k))){ present[i] = true; }
				else{ present[i] = false; }
				i++;
			}
			// check total compliance. if all matched, just return true.
			// if any are false, continue iterations until exhausted. 
			retval = true;
			for(int count = 0; count < present.length; count++){
				retval = present[count] && retval;
			}
			if(retval){
				break; // this break will cause a return of true. 
			}
		}
		// tell the caller. 
		return retval;
	}
	
	/**
	 * return an extract of the content of the DispatchQueryGroup
	 * @return
	 * @throws Exception 
	 */
	public ArrayList<ArrayList<String>> extractMappings(String [] headers) throws Exception{
		ArrayList<ArrayList<String>> retval = new ArrayList<ArrayList<String>>();
		
		// get the Sem headers;
		Set<String> semkeys = this.semanticColumnValues.keySet();
		
		// for each EDC value set, create a new row 
		for(HashMap<java.lang.String, java.lang.String> edcCurr : this.edcColumnValues){ // not sure why the namesace for string had to be given. it threw an error when not.
			ArrayList<String> currRow = new ArrayList<String>();
			// put everything in order. 
			currRow.add(this.guid.toString());			// note: the GUID is always the first element.
			
			Set<String> edcKeys = edcCurr.keySet();
			
			// get each next value
			for(String hNow : headers){
				if(edcKeys.contains(hNow)){  // check if it is part of the EDC
					currRow.add(edcCurr.get(hNow));
				}
				
				else if (semkeys.contains(hNow)){ // check if it is part of the Sem
					currRow.add(this.semanticColumnValues.get(hNow));
				}
				
				else { // panic
					throw new Exception("column " + hNow + " not found in any data related to subquery " + this.guid);
				}
			}
			// add it to the output
			retval.add(currRow); // add it to the collection
		}
		// ship it out.
		return retval;
	}
	/**
	 * add additional results o the known collection. once column info has been added once, you can just add more data.
	 * this assumes that all sets have the same number of columns and are ordered the same way.  
	 * @param res
	 * @throws Exception
	 */
	public synchronized void addResults(Table res) throws Exception{
		
		this.partialResults.add(res);
		
	}
	
	public ArrayList<Table> getPartialResults(){
		return this.partialResults;
	}
	
	/**
	 * using columnsInOrder, create an export of the partial results in this group, complete with the semantic results fused in
	 * @param columnsInOrder
	 * @return
	 */
	// Appears to be dead code: PEC 6/13/2018
	public ArrayList<ArrayList<String>> exportResultsInOrder(String[] columnsInOrder){
		ArrayList<ArrayList<String>> retval = new ArrayList<ArrayList<String>>();
		
		for(Table currPartial : this.partialResults){
			// spin through each row and build our result. 
			for(ArrayList<String> row : currPartial.getRows()){
				
				ArrayList<String> outRow = new ArrayList<String>();
				
				// get the values for each column we care about. things are about to get big. 
				for(String currCol : columnsInOrder){
					if(this.semanticColumnValues.keySet().contains(currCol)){
						// get the data from the semantic values.
						outRow.add(this.semanticColumnValues.get(currCol));
					}
					else{
						try{
							outRow.add(row.get(currPartial.getColumnIndex(currCol)));
						}
						catch(Exception e){
							outRow.add(" --- ");
						}
					}
				}
				retval.add(outRow);
			}
		}
		return retval;
	}
	
	
	public void exportResultsInOrder_Parallel(String[] columnsInOrder, int initialOffset, Object[] retval){		
		int rollingOffset = 0;
		
		QueryGroupFusionWorkerThread[] waitingOn = new QueryGroupFusionWorkerThread[this.partialResults.size()];
			
		int ii = 0;
		for(Table currPartial : this.partialResults){
			int offset = initialOffset + rollingOffset;
			QueryGroupFusionWorkerThread curr = new QueryGroupFusionWorkerThread(currPartial, offset, retval, columnsInOrder, this.semanticColumnValues);
			
			waitingOn[ii] = curr;
			curr.start();
			ii += 1;
			
			rollingOffset += currPartial.getNumRows();
		}
		
		// check for all threads joined. 
		for(int k = 0; k < waitingOn.length; k += 1){
			try {
				waitingOn[k].join();
			} catch (InterruptedException e) {
				LocalLogger.printStackTrace(e);
			}
		}
		
	}
	
	public int getTotalResultOffset(){
		int retval = 0;
		
		for(Table curr : this.partialResults){
			retval += curr.getNumRows();
		}
		
		return retval;
	}
	
	
	/**
	 * return the names of the columns in the partial results set. 
	 * @return
	 */
	public ArrayList<String> getPartialResultsColumnNames(){
		ArrayList<String> retval = null;
		
		if(this.partialResults != null){
			retval = new ArrayList<String>();
			// spin through the array and return the names we wanted. 
			for(Table pr : this.partialResults){
			

				for(String currColName : pr.getColumnNames()){
					if(!retval.contains(currColName)){
						retval.add(currColName);
					}
					
				}
			}
		}
		return retval;
	}
	/**
	 * return the names of the semantic columns used to generate results. 
	 * @return
	 */
	public ArrayList<String> getSemanticColumnNames() {
		ArrayList<String> retval = null;
		
		if(this.semanticColumnValues != null){  // we have inputs so we can return something. 
			retval = new ArrayList<String>();
			for(String key : this.semanticColumnValues.keySet()){
				retval.add(key);
			}
		}
		return retval;
	}
	/**
	 * returns true if a partial result is available.
	 * @return
	 */
	public Boolean partialResultsExist(){
		Boolean retval = false;
		if(this.partialResults != null){ retval = true;}
		else{ retval = false;}
		return retval;
	}
	/**
	 * for a column name, return the type of that column. if the name does not exist, return null
	 * @param columnName
	 * @return
	 */
	public String getPartialResultsColumnType(String columnName){
		String retval = null;
		
		if(this.partialResults != null){
			
			for(Table currTabl : this.partialResults){
			
				try{
					retval = currTabl.getColumnType(columnName);
				}
				catch(Exception eee){
					retval = null;
				}
				// we only need one.
				if(retval != null){ break; }
			}
		}
		
		return retval;
	}
	
}
