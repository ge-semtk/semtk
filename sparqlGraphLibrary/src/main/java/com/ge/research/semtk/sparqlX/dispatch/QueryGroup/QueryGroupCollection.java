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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utilityge.Utility;


public class QueryGroupCollection {

	private HashMap<UUID, DispatchQueryGroup> dispatchQueryGroupHash;
	private HashMap<String, Integer> edcColNumHash;
	private HashMap<String, Integer> semanticColNumHash;
	private HashMap<String, String> semanticColTypeHash;
	
	public QueryGroupCollection(){
		this.dispatchQueryGroupHash = new HashMap<UUID, DispatchQueryGroup>();
		this.edcColNumHash = new HashMap<String, Integer>();
		this.semanticColNumHash = new HashMap<String, Integer>();
		this.semanticColTypeHash = new HashMap<String, String>();
	}
	
	public QueryGroupCollection(Table sparqlResults, String[] edcColumnNames, ArrayList<String> semanticColumnNames) throws Exception{
		this();
		this. buildQueryGroupCollection(sparqlResults, edcColumnNames, semanticColumnNames);
	}
	
	/**
	 * build the query group collection to be used. this is largely automated to remove potential user error/inaccuracy
	 * @param sparqlResults
	 * @param edcColumnNames
	 * @param semanticColumnNames
	 * @throws Exception 
	 */
	public void buildQueryGroupCollection(Table sparqlResults, String[] edcColumnNames, ArrayList<String> semanticColumnNames) throws Exception{
		// given these values, create an appropriately binned collection
		
		// set up the this.semanticColumnNumbers and this.edcColumnNumbers
		this.prepareEdcColumnIndices(edcColumnNames, sparqlResults);
		this.prepareSemanticColumnIndices(semanticColumnNames, sparqlResults);
		
		// step through all rows in table
		for(ArrayList<String> currRow : sparqlResults.getRows()){
			// get the semantics-only values
			HashMap<String, String> currSemVals = new HashMap<String, String>();
			
			for(String colName : this.semanticColNumHash.keySet()){
				// build hash<col_name><cell_value>
				currSemVals.put(colName, currRow.get(this.semanticColNumHash.get(colName)));
			}
			
			// get the edc-required values
			HashMap<String, String> currEdcVals = new HashMap<String, String>();
			
			for(String colName : this.edcColNumHash.keySet()){
				// build hash<col_name><cell_value>
				currEdcVals.put(colName, currRow.get(this.edcColNumHash.get(colName)));
			}
						
			// check if exist in a query group. add if not existing at all, modify if there is an appropriate set
			this.modifyOrCreateDispatchQueryGroup(currSemVals, currEdcVals);
			
		}
	}

	/**
	 * match the incoming column numbers to the values we care about from the original table.
	 * this is to later simplify lookups
	 * @param semCol
	 * @param res
	 */
	private void prepareSemanticColumnIndices(ArrayList<String> semCol, Table res){
		for(String s : semCol){
			this.semanticColNumHash.put(s, res.getColumnIndex(s));
		}
	}
	/**
	 * match the incoming column numbers to the values we care about from the original table.
	 * this is to later simplify lookups
	 * @param semCol
	 * @param res
	 * @throws Exception 
	 */
	private void prepareEdcColumnIndices(String[] edcCol, Table res) throws Exception{
		for(String s : edcCol){
			int index = res.getColumnIndex(s);
			if(index > -1){
				this.edcColNumHash.put(s, res.getColumnIndex(s));
			}else{
				throw new Exception("Cannot find expected EDC column '" + s + "' in table: " + res.toCSVString());
			}
		}
	}
	
	/**
	 * Make sure there is one DispatchQueryGroup for each unique set of semantic data.
	 * Attach an EdcColumnSet for each unique set of edc data.
	 * 
	 * NOTE:  Paul/Justin Jan2018
	 * This is probably backwards.  This algorithm can result in the same set of edc data
	 * being sent out multiple times.
	 * (1) each edc set should only be sent out once
	 * (2) edc config could provide efficiency help:  
	 * 		e.g. send all rows with same location to one edc call.  (TEDS)
	 *             vs.
	 *           send all rows with the same tag to one edc call.
	 * 
	 * using the incoming semanticResults HashMap, check for the existence of a DispatchQueryGroup which already contains that data.
	 * if one exists, add the 'new' edcResults info to it, if applicable.
	 * if no appropriate DispatchQueryGroup can be located, create a new one and add it to the collection. 
	 * @param semanticResults
	 * @param edcResults
	 * @return
	 */
	private DispatchQueryGroup modifyOrCreateDispatchQueryGroup(HashMap<String, String> semanticResults, HashMap<String, String> edcResults){
		DispatchQueryGroup retval = null;
		
		// check for the existence of DispatchQueryGroup that has the semantic info we care about. 
		for(DispatchQueryGroup currDqg : this.dispatchQueryGroupHash.values()){
			if(currDqg.semanticMatchesInput(semanticResults)){
				// found it. 
				retval = currDqg;
				break;
			}
		}		

		if(retval != null){
			// if retval is not null, we have found a match. a positive match means we can move on and modify an existing Dispatch Query Group
			retval.addEdcColumnSet(edcResults);
		}
		else{
			// not found... make a new one. 
			retval = new DispatchQueryGroup();
			retval.addSemanticColumnValues(semanticResults);
			retval.addEdcColumnSet(edcResults);
			this.dispatchQueryGroupHash.put(retval.getUUID(), retval);
		}
		
		return retval;
	}
	/**
	 * create a table for one call to a query generator.
	 * Semantic data is collapsed into a single UUID column.
	 * Each UUID may have multiple sets of edc values (i.e. multiple rows) 
	 * DispatchQueryGroup collection elements. 
	 * @return
	 * @throws Exception 
	 */
	public Table returnDispatchQueryGroupTable() throws Exception{
		Table retval = null;
		// get a column ordering. create an array to hold all the names
		String[] headers = new String[this.edcColNumHash.size()];
		String[] headerTypes = new String[this.edcColNumHash.size() + 1];
		// populate the headers
		int counter = 0;
		for(String keyEdc : this.edcColNumHash.keySet()){
			// add
			headers[counter] = keyEdc;
			counter++;
		}
		
		// dig through the collection and get extracted columns. 
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		for(DispatchQueryGroup dqg : this.dispatchQueryGroupHash.values()){
			// get rows.
			ArrayList<ArrayList<String>> temp = dqg.extractMappings(headers);
			// add what we found. 
			for(ArrayList<String> k : temp){
				rows.add(k);
			}
		}
		
		// add UUID to the header row. make sure it is first. 
		String[] completeheaders = new String[headers.length + 1];
		completeheaders[0] = Utility.COL_NAME_UUID;
		headerTypes[0] = "String";
		for(int i = 0; i < headers.length; i++){
			completeheaders[i+1] = headers[i];
			headerTypes[i+1] = "String";
		}
		retval = new Table(completeheaders, headerTypes, rows);
		return retval;
	}
	/**
	 * for the DispatchQueryGroup referenced by the given UUID, add the results. 
	 * @param guid
	 * @param res
	 * @throws Exception
	 */
	public void addResults(UUID guid, Table res) throws Exception{
		DispatchQueryGroup desired = this.dispatchQueryGroupHash.get(guid);
		desired.addResults(res);
	}	
	/**
	 * take in the desired columns and types and return all of the partial results as a single result set. 
	 * @param columnNamesInOrder
	 * @param columnTypesInNameOrder
	 * @return
	 * @throws Exception
	 */
	// Appears to be dead code: PEC 6/13/2018
	public Table returnFusedResults(String[] columnNamesInOrder, String[] columnTypesInNameOrder) throws Exception{
		Table retval = null;
		
		// interrogate each DQG for its results. 
		ArrayList<ArrayList<String>> tableRows = new ArrayList<ArrayList<String>>();
		for(DispatchQueryGroup dqg : this.dispatchQueryGroupHash.values()){
			tableRows.addAll(dqg.exportResultsInOrder(columnNamesInOrder));
		}
		// create a new table to return results into. 
		retval = new Table(columnNamesInOrder, columnTypesInNameOrder, tableRows);
		// ship it out.
		return retval;
	}
	
	public Table returnFusedResults_parallel(String[] columnNamesInOrder, String[] columnTypesInNameOrder) throws Exception{

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		// allocate blank results and rows
		Table retval = null;
		ArrayList<ArrayList<String>> tableRows = new ArrayList<ArrayList<String>>();
		
		// get sum of rows in each returned table
		int fullSet = 0;
		for(DispatchQueryGroup d : this.dispatchQueryGroupHash.values()){
			fullSet += d.getTotalResultOffset();
		}
		
		LocalLogger.logToStdOut("full result set size is expected to be: " + fullSet);
	
		LocalLogger.logToStdErr("about to start running fusion threads @ " + dateFormat.format(Calendar.getInstance().getTime()));

		// create results as array instead of ArrayList so threads don't compete
		// WARNING:  MEMORY CRUNCH OCCURS HERE
		Object[] tempStorage = new Object[fullSet];
		
		// create thread for each DispatchQueryGroup 
		int allThreads = 0;
		OverallFusionWorkerThread[] threads = new OverallFusionWorkerThread[this.dispatchQueryGroupHash.values().size()];
		int startOffset = 0;
		
		// launch threads, which write to tempStorage
		for(DispatchQueryGroup dqg : this.dispatchQueryGroupHash.values()){
			
			OverallFusionWorkerThread currThread = new OverallFusionWorkerThread(dqg, startOffset, columnNamesInOrder, tempStorage);
			threads[allThreads] = currThread;
			
			currThread.start();
			allThreads += 1;
			// calculate offset for next thread
			startOffset += dqg.getTotalResultOffset();
		}
		
		// wait on the results...
		for(int k = 0; k < threads.length; k += 1){
			try {
				threads[k].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				LocalLogger.printStackTrace(e);
			}
		}
		LocalLogger.logToStdErr("done running fusion threads. about to add all rows to result set. @ " + dateFormat.format(Calendar.getInstance().getTime()));
		
		// add everything from tempStorage to tableRows...
		for(Object n : tempStorage){
			ArrayList<String> currRow = (ArrayList<String>)n;
			tableRows.add(currRow);
		}
		
		LocalLogger.logToStdErr("done adding results to set.@ " + dateFormat.format(Calendar.getInstance().getTime()));
		
		// create a new table containing tableRows 
		retval = new Table(columnNamesInOrder, columnTypesInNameOrder, tableRows);
		
		LocalLogger.logToStdErr("Result table build finished @ " + dateFormat.format(Calendar.getInstance().getTime()));
		
		return retval;
	}
	
	/**
	 * get all of the edc column names. This exploits the fact that all the subqueries have the same loadout.
	 * @return
	 */
	private ArrayList<String> getEdcResultsColumnNames(){
		ArrayList<String> retval = new ArrayList<String>();
		// look for one that seems reasonably complete. 
		for(DispatchQueryGroup dqg : this.dispatchQueryGroupHash.values()){
			ArrayList<String> temp = dqg.getPartialResultsColumnNames();
			if(temp != null){
				// really, we only need to look at each working one. this is because different sub-queries might have different covered values
				for(String s : temp){
					// this is sort of inefficient but is included to maintain column ordering. 
					// using a hashmap is probably the preferred mechanism but leads to nondeterministic column ordering on output. 
					// users see this as undesirable behavior so i implemented a lower performing, more predictable solution.
					// a point of note here is that the order is weirdly determined by the first partial result for primary order and then
					// additional values are controlled by the order in which they appear given processing of the other partial results.
					// this leads to a situation where external values order is controlled indirectly by query generation and execution selecting the
					// value positions. also, there is no way to order semantic results. sorry.
					// -Justin
					if(!retval.contains(s)){
						// add it.
						retval.add(s);
					}
				}
			}
		}
		
		return retval;
	}
	/**
	 * get all of the semantic column names. This exploits the fact that all the subqueries have the same loadout.
	 * @return
	 */
	private ArrayList<String> getSemanticColumnNames(){
		ArrayList<String> retval = null;
		// look for one that seems reasonably complete. 
		for(DispatchQueryGroup dqg : this.dispatchQueryGroupHash.values()){
			retval = dqg.getSemanticColumnNames();
			if(retval != null){
				// really, we only need one working one. just cut out if a return is complete. 
				break;
			}
		}
		return retval;	
	}
	/**
	 * return all of the columns we care about for this query set. the semantic ones are listed first 
	 * @return
	 */
	public String[] getColumnNames(){		
		
		ArrayList<String> tempRet = this.getSemanticColumnNames();
		
		ArrayList<String> edcRes = this.getEdcResultsColumnNames();
		if (edcRes != null) {
			tempRet.addAll(getEdcResultsColumnNames());
		}
		
		String[] retval = new String[tempRet.size()];
		return tempRet.toArray(retval);   
		
	}
	
	public int getSemanticColumnCount(){
		return this.getSemanticColumnNames().size();
	}
	
	/**
	 * returns a given DispatchQueryGroup by UUID.
	 * @param guid
	 * @return
	 */
	public DispatchQueryGroup getGroupByUUID(UUID guid){
		DispatchQueryGroup retval = null;
		if(this.dispatchQueryGroupHash.keySet().contains(guid)){
			retval = this.dispatchQueryGroupHash.get(guid);
		}
		return retval;
	}
	/**
	 * set the types for a semantic column.
	 * @param columnName
	 * @param columnType
	 */
	public void setSemanticColumnType(String columnName, String columnType){
		this.semanticColTypeHash.put(columnName, columnType);
	}
	/**
	 * for a given column name, return the associated type. if the name is not valid, a null string is returned. 
	 * @param columnName
	 * @return
	 */
	public String getColumnType(String columnName){
		String retval = null;

		// check the semantic columns first.
		if(this.semanticColTypeHash.keySet().contains(columnName)){ retval = this.semanticColTypeHash.get(columnName);}
		else{
			for(DispatchQueryGroup dqg : this.dispatchQueryGroupHash.values()){
				if(dqg.partialResultsExist()){
					retval = dqg.getPartialResultsColumnType(columnName);
				}
				if(retval != null){
					// really, we only need one working one. just cut out if a return is complete. 
					break;
				}
			}
		}
		
		return retval;
	}
}
 