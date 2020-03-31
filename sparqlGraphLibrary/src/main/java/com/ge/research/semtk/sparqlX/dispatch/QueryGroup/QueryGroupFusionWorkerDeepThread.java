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

public class QueryGroupFusionWorkerDeepThread extends Thread {

	public ArrayList<ArrayList<String>> myParentsPartialResultsRows;
	public String[] mySemanticValues;
	public int myStartOffset;
	public int myEndOffset;
	public int myOutputOffset;
	public Integer[] myControlRow;
	
	public Object[] globalResultSetReference;
	
	
	public QueryGroupFusionWorkerDeepThread(ArrayList<ArrayList<String>> incomingRows, int startPosition, int offsetToEndPosition, int outputOffset, 
			Integer[] rowController, String[] semanticValues, Object[] globalResults){
		
		this.myParentsPartialResultsRows = incomingRows;
		this.myStartOffset = startPosition;
		this.myEndOffset = startPosition + offsetToEndPosition;
		this.myControlRow = rowController;
		this.mySemanticValues = semanticValues;
		this.globalResultSetReference = globalResults;
		this.myOutputOffset = outputOffset;
		
		//LocalLogger.logToStdOut("deep worker thread initialized with a starting offset of: " + this.myStartOffset + " and an end of: " + this.myEndOffset);
	}
	
	public void run(){
	
		// do the actual work:
		int fullResultSize = myParentsPartialResultsRows.size();
				
		for(int currentPosition = this.myStartOffset; currentPosition < this.myEndOffset; currentPosition++){

			if(currentPosition < fullResultSize){  // only work if there is still work. 
				ArrayList<String> outRow = new ArrayList<String>();
				ArrayList<String> row = this.myParentsPartialResultsRows.get(currentPosition);
				
				// do the work for this row:
				for(Integer currValHolder : this.myControlRow){
					// if it is null, just add the sentinel value.
					if(currValHolder == null){ outRow.add("---"); }
					
					// if it is negative, use the semantic column value for this block
					else if(currValHolder < 0){ outRow.add(this.mySemanticValues[-1*currValHolder]); }
					
					// else just use the value in that position of the incoming row array list.
					else { outRow.add(row.get(currValHolder)); }
				}
					
				
				this.globalResultSetReference[this.myOutputOffset] =  (Object)outRow;
				this.myOutputOffset++;
			}
			else{
				break;
			}
			
		}
		
	}
	
}
