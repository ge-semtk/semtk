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
package com.ge.research.semtk.sparqlX.dispatch;

import java.util.ArrayList;

import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;

public class ResultSetHandler {
	// handle the collection of result sets that come back from the service invocations.
	
	private ArrayList<GeneralResultSet> resultSets = new ArrayList<GeneralResultSet>(); 
	
	
	public ResultSetHandler(){
		// just a basic constructor
	}
	/**
	 * add a new result set to the collection. 
	 * check to make sure that the current results (if any) are not a mismatch for the incoming ones.
	 * if so, toss an exception. 
	 * @param curr
	 * @throws ResultSetTypeMismatchException 
	 * @throws ResultSetTypeFailureException 
	 */
	public void addResults(GeneralResultSet curr) throws ResultSetTypeMismatchException, ResultSetTypeFailureException{
		// check for obvious bad types... ones we cannot really fuse. 
		if(curr.getResultsBlockName().equalsIgnoreCase(SimpleResultSet.RESULTS_BLOCK_NAME)){
			throw new ResultSetTypeFailureException("simple result sets cannot be fused.");
		}
		
		if(this.resultSets.size() > 0){
			String existingBlockName = this.resultSets.get(0).getResultsBlockName();
			if(!curr.getResultsBlockName().equalsIgnoreCase(existingBlockName)){
				// the existing and new block names do not match. oops. error.
				throw new ResultSetTypeMismatchException("existing block type of results (" + existingBlockName + ") does not match incoming value (" + curr.getResultsBlockName() + "). is it possible that values from multiple queries are beign inappropriately fused?");
			}	
		}
		// things went fine... add it.
		this.resultSets.add(curr);
	}
	
	/**
	 * take the collection of the sets and fuse them into a single set. this is only valid over types that
	 * can be fused. that means that simple result set is not going to have a fused result. 
	 * @return
	 * @throws Exception 
	 */
	public GeneralResultSet fuseResults() throws Exception{
		GeneralResultSet retval = null;
		
		if(this.resultSets.size() == 1){
			// just return whatever we have. this bypasses a lot of questions about subtype validity for fusion but does so because they are
			// no longer important. 
			retval = this.resultSets.get(0);
		}
		// check for the type and them call the appropriate one. 
		else if(this.resultSets.size() > 1){
			// we have at least one result. move on.
			if(this.resultSets.get(0).getResultsBlockName().equalsIgnoreCase(TableResultSet.RESULTS_BLOCK_NAME)){
				// merge all tables
				ArrayList<TableResultSet> tableResultSets = new ArrayList<TableResultSet>();
				for(GeneralResultSet rs : resultSets){
					tableResultSets.add((TableResultSet) rs);
				}
				retval = TableResultSet.merge(tableResultSets);				
			}
			else if(this.resultSets.get(0).getResultsBlockName().equalsIgnoreCase(NodeGroupResultSet.RESULTS_BLOCK_NAME)){
				// merge all node groups 
				throw new Exception("Node group result set merge not implemented yet"); // TODO
			}
			else{
				throw new FusionNotAvailableException("Desired type (" + this.resultSets.get(0).getResultsBlockName() + ") has no implemented fusion method.");
			}
		}
		// ship it out. 
		return retval;
	}
	
	public TableResultSet fuseTableResults(){
		// TODO: implement later
		return null;
	}

	public NodeGroupResultSet fuseNodeGroupResults(){
		// TODO: implement later
		return null;
	}
}
