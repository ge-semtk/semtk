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

import java.util.ArrayList;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataSetExhaustedException;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
/*
 * Takes multiple data records and uses them to populate NodeGroups.
 */
public class DataToModelTransformer {

	Dataset ds = null;
	int batchSize = 1;
	ImportSpecHandler importSpec = null;
	OntologyInfo oInfo = null;
	Table failuresEncountered = null;
	int totalRecordsProcessed = 0;
	
	// TODO: passing through the endpoint is questionable & temporary for POC
	public DataToModelTransformer(SparqlGraphJson sgJson, SparqlEndpointInterface endpoint) throws Exception{
		this.oInfo = sgJson.getOntologyInfo();
		this.importSpec = sgJson.getImportSpec();
		this.importSpec.setEndpoint(endpoint);
		
		if (sgJson.getImportSpecJson() == null) {
			throw new Exception("The data transformation import spec is null.");
		}
		if (sgJson.getSNodeGroupJson() == null) {
			throw new Exception("The data transformation nodegroup is null.");
		}
	}

	public DataToModelTransformer(SparqlGraphJson sgJson, int batchSize, SparqlEndpointInterface endpoint) throws Exception{
		this(sgJson, endpoint);
		this.batchSize = batchSize;
	}

	public DataToModelTransformer(SparqlGraphJson sgJson, int batchSize, Dataset ds, SparqlEndpointInterface endpoint) throws Exception{
		this(sgJson, endpoint);
		this.batchSize = batchSize;
		this.setDataset(ds);
	}
	
	public String [] getImportColNames() {
		return importSpec.getColNamesUsed();
	}
	
	public void setDataset(Dataset ds) throws Exception{
		this.ds = ds;
		if(ds != null){
			this.importSpec.setHeaders(ds.getColumnNamesinOrder());
			
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
	
	public ArrayList<NodeGroup> getNextNodeGroups(int nRecordsRequested, boolean skipValidation) throws Exception {
		ArrayList<NodeGroup> nGroups;		// this will be filled out and returned. 
		
		ArrayList<ArrayList<String>> resp = this.ds.getNextRecords(nRecordsRequested);
		
		if(resp.size() == 0){
			// no new records
			throw new DataSetExhaustedException("No more records to read");
		}
		
		// add the values to the NodeGroup...
		nGroups = this.convertToNodeGroups(resp, skipValidation);
		
		return nGroups;
	}

	public ArrayList<NodeGroup> getNextNodeGroupBatch(boolean skipValidation) throws Exception {
		return this.getNextNodeGroups(this.batchSize, skipValidation);	
	}

	private ArrayList<NodeGroup> convertToNodeGroups(ArrayList<ArrayList<String>> resp, boolean skipValidation) throws Exception {
		// take the response we received and build the result node groups we care about.
		ArrayList<NodeGroup> retval = new ArrayList<NodeGroup>();
		
		// if cannot read more records (e.g. if all records have been read already), nothing to do
		if(resp == null){
			return retval;
		}
		
		for(ArrayList<String> curr : resp){
			this.totalRecordsProcessed += 1;
			
			// get our new node group
			NodeGroup cng = null;
			
			// add the values from the results to it.
			try{
				cng = this.importSpec.buildImportNodegroup(curr, skipValidation);
			
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
