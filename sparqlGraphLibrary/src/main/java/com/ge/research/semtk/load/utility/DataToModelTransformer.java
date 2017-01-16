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


package com.ge.research.semtk.load.utility;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataSetExhaustedException;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
/*
 * Takes data records and uses them to populate NodeGroups.
 */
public class DataToModelTransformer {

	Dataset ds = null;
	int batchSize = 1;
	JSONObject basisNodegroupJson;
	ImportSpecHandler transformSpec = null;
	OntologyInfo oInfo = null;
	Table failuresEncountered = null;
	int totalRecordsProcessed = 0;
	
	public DataToModelTransformer(SparqlGraphJson sgJson) throws Exception{
		this.basisNodegroupJson = sgJson.getSNodeGroupJson();
		this.oInfo = sgJson.getOntologyInfo();
		this.transformSpec = sgJson.getImportSpec();
		
		if(basisNodegroupJson == null || transformSpec == null){
			// none of this is valid. panic and throw exception.
			throw new Exception("either the basis nodegroup or the data transformation spec were null.");
		}
	}

	public DataToModelTransformer(SparqlGraphJson sgJson, int batchSize) throws Exception{
		this(sgJson);
		this.batchSize = batchSize;
	}

	public DataToModelTransformer(SparqlGraphJson sgJson, int batchSize, Dataset ds) throws Exception{
		this(sgJson);
		this.batchSize = batchSize;
		this.setDataset(ds);
	}
	
	public String [] getImportColNames() {
		return transformSpec.getColNamesUsed();
	}
	
	public void setDataset(Dataset ds) throws Exception{
		this.ds = ds;
		if(ds != null){
			this.transformSpec.setHeaders(ds.getColumnNamesinOrder());
			
			ArrayList<String> failureCols = this.ds.getColumnNamesinOrder();
			failureCols.add(DataLoader.FAILURE_CAUSE_COLUMN_NAME);
			failureCols.add(DataLoader.FAILURE_RECORD_COLUMN_NAME);
			ArrayList<String> failureColTypes = new ArrayList<String>();

			for(int cntr = 0; cntr < failureCols.size(); cntr++){
				failureColTypes.add("String");
			}
			
			String[] failureColsArray = new String[failureCols.size()];
			failureColsArray = failureCols.toArray(failureColsArray);
			String[] failureColTypesArray = new String[failureColTypes.size()];
			failureColTypesArray = failureColTypes.toArray(failureColTypesArray);

			this.failuresEncountered = new Table(failureColsArray, failureColTypesArray, null);
		}
		else{
			throw new Exception("dataset cannot be null");
		}
	}
	
	public void resetDataSet() throws Exception{
		this.ds.reset();
		this.totalRecordsProcessed = 0;
		this.failuresEncountered.clearRows();
	}
	
	public void setBatchSize(int bSize){
		this.batchSize = bSize;
	}
	
	private ArrayList<NodeGroup> getNextNRecords(int nRecordsRequested) throws Exception {
		ArrayList<NodeGroup> nGroups;		// this will be filled out and returned. 
		
		ArrayList<ArrayList<String>> resp = this.ds.getNextRecords(nRecordsRequested);
		
		if(resp.size() == 0){
			// no new records
			throw new DataSetExhaustedException("No more records to read");
		}
		
		// add the values to the NodeGroup...
		nGroups = this.convertToNodeGroups(resp);
		
		return nGroups;
	}
	
	public ArrayList<NodeGroup> getNext() throws Exception {
		return this.getNextNRecords(1);
	}

	public ArrayList<NodeGroup> getNextBatch() throws Exception {
		return this.getNextNRecords(this.batchSize);	
	}

	private ArrayList<NodeGroup> convertToNodeGroups(ArrayList<ArrayList<String>> resp) throws Exception {
		// take the response we received and build the result node groups we care about.
		ArrayList<NodeGroup> retval = new ArrayList<NodeGroup>();
		
		// if cannot read more records (e.g. if all records have been read already), nothing to do
		if(resp == null){
			return retval;
		}
		
		for(ArrayList<String> curr : resp){
			this.totalRecordsProcessed += 1;
			
			// get our new node group
			NodeGroup cng = NodeGroup.getInstanceFromJson(basisNodegroupJson);
			
			// add the values from the results to it.
			try{
				cng = this.transformSpec.importRecord(cng, curr);
			
				// add the new group to the output arraylist, only if it succceeded
				retval.add(cng);
			}
			catch(Exception e){
				// some variety of failure occured.
				ArrayList<String> newErrorReport = new ArrayList<String>();
				// add default columns
				for(String currCol : curr){
					newErrorReport.add(currCol);
				}
				// add error report columns
				newErrorReport.add(e.getMessage());
				newErrorReport.add(this.totalRecordsProcessed + "");
				this.failuresEncountered.addRow(newErrorReport);
			}
		}
			
		return retval;
	}

	public void closeDataSet() throws Exception {
		this.ds.close();
	}
	
	public Table getErrorReport(){
		return this.failuresEncountered;
	}
}
