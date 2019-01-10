/**
 ** Copyright 2017 General Electric Company
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

package com.ge.research.semtk.edc.resultsStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

import javax.activation.UnsupportedDataTypeException;
import javax.lang.model.type.UnknownTypeException;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.resultsStorage.TableResultsStorage.TableResultsStorageTypes;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;

public class TableResultsSerializer {
	
	private static final int FLUSHFREQUENCY = 100;
	private JSONObject headerInfo = null;
	private File dataFile = null;
	private TableResultsStorageTypes frmt = null;
	private Integer cutoffValue = null;
	private Integer startingRowNumber = 0;	// all tables are based on row 0.
	
	public TableResultsSerializer(){
		
	}
	public TableResultsSerializer(JSONObject headerInfo, String dataFileLocation, TableResultsStorageTypes serializationType, Integer cutoff, Integer startingRow){
		 
		this.headerInfo = headerInfo;
		this.dataFile = new File(dataFileLocation);
		this.frmt = serializationType;
		this.cutoffValue = cutoff;
		if(startingRow == null || startingRow < 0){ this.startingRowNumber = 0; }
		else{ this.startingRowNumber = startingRow; }
	}
		
	public void writeToStream(PrintWriter printWriter ) throws IOException, UnsupportedOperationException {
		this.writeObject(printWriter);
	}
	private void writeObject( PrintWriter aOutputStream ) throws IOException, UnsupportedOperationException {
	
		// this method actually cheats and writes the output as the desired type... 
		// not as a true internal representation of the data.
		Integer totalRowsExpected   = Integer.parseInt( (this.headerInfo.get(Table.JSON_KEY_ROW_COUNT)).toString() );
		// determine how much to return
		if(this.cutoffValue != null && totalRowsExpected != null && this.cutoffValue > totalRowsExpected){
			this.cutoffValue = totalRowsExpected - this.startingRowNumber;
		}
		else if(this.cutoffValue == null){ this.cutoffValue = totalRowsExpected - this.startingRowNumber; }
		else if(this.cutoffValue >  totalRowsExpected - this.startingRowNumber)
		{ this.cutoffValue = totalRowsExpected - this.startingRowNumber; }
		
		LocalLogger.logToStdErr("requested file record size = " + cutoffValue);
		
		if(this.frmt.equals(TableResultsStorageTypes.JSON)){
			// json code call.
			
			this.writeJSON(aOutputStream, cutoffValue);
		}
		else if(this.frmt.equals(TableResultsStorageTypes.CSV)){
			// csv code call
			
			this.writeCSV(aOutputStream, cutoffValue);
		}
		else{
			throw new UnsupportedOperationException("TableResultsSerializer.serializationFormat." + this.frmt.name() + " is not supported for serialization. version mismatch?");
		}
		
    }
	
	private void writeCSV( PrintWriter aOutputStream, Integer stopRowNumber ) throws UnsupportedOperationException, IOException{
		
		String quote = "\"";
		
		if(this.headerInfo == null){ throw new UnsupportedOperationException("cannot return info when metadata is empty or nonexistent"); }
		// open the data file
		if(!this.dataFile.exists()){ throw new UnsupportedOperationException("cannot return info when data file is nonexistent"); }
		
		// write the metadata to the stream
		int columnCount = Integer.parseInt( "" + this.headerInfo.get(Table.JSON_KEY_COL_COUNT));
				
		JSONArray jArr = (JSONArray) this.headerInfo.get(Table.JSON_KEY_COL_NAMES);
		for(int colCount= 0; colCount < columnCount; colCount++){
			aOutputStream.write((String)jArr.get(colCount));
			if(colCount != columnCount - 1){
				// we need a comma
				aOutputStream.write(",");
			}
		}
		aOutputStream.write("\n");
		
		// done with metadata. flush.
		aOutputStream.flush();
		
		// process the data file rows until the cutoff is reached.
		int processedRows = 0;
		boolean endOfInput = false;
		
		FileReader fr = new FileReader(dataFile);
		BufferedReader bfr = new BufferedReader(fr);
		
		// fast foward
		bfr = this.fastForwardResultsFile(bfr);
		
		
		while(processedRows < stopRowNumber && !endOfInput){
			// read the next row from the data set and write to the stream. 

			String currRow = bfr.readLine();
			// conversion should not be required in this case as it was read as written.
			aOutputStream.write(currRow.substring(1, currRow.length() - 1));
			
			// add the comma, if needed.
			if(processedRows < stopRowNumber -1){  aOutputStream.write("\n"); }
			
			processedRows += 1;
			
			if(processedRows % FLUSHFREQUENCY == 0){
				LocalLogger.logToStdErr("flushing after row: " +  processedRows);
				aOutputStream.flush();
			}
		}
		bfr.close();
		
		// done with rows. flush.
		aOutputStream.flush();
		LocalLogger.logToStdErr("flushing after completion: " +  processedRows);
	}
	
	private void writeJSON( PrintWriter aOutputStream, Integer stopRowNumber) throws UnsupportedOperationException, IOException{
		boolean done = false;
		
		String quote = "\"";
		
		if(this.headerInfo == null){ throw new UnsupportedOperationException("cannot return info when metadata is empty or nonexistent"); }
		// open the data file
		if(!this.dataFile.exists()){ throw new UnsupportedOperationException("cannot return info when data file is nonexistent"); }
		
		// write the metadata to the stream
		int columnCount = Integer.parseInt( "" + this.headerInfo.get(Table.JSON_KEY_COL_COUNT));
		
		LocalLogger.logToStdErr("requested row count : " + stopRowNumber);
		
		aOutputStream.write("{" + quote + Table.JSON_KEY_ROW_COUNT + quote + " : "  + stopRowNumber + ",");
		aOutputStream.write(quote + Table.JSON_KEY_COL_COUNT + quote + " : "  + columnCount + ",");
		aOutputStream.write(quote + Table.JSON_KEY_COL_NAMES + "\" : [");
		
		JSONArray jArr = (JSONArray) this.headerInfo.get(Table.JSON_KEY_COL_NAMES);
		for(int colCount= 0; colCount < columnCount; colCount++){
			aOutputStream.write(quote + jArr.get(colCount) + quote);
			if(colCount != columnCount - 1){
				// we need a comma
				aOutputStream.write(",");
			}
		}
		aOutputStream.write("],");
		
		aOutputStream.write(quote + Table.JSON_KEY_COL_TYPES + quote + " : [");
		JSONArray jArrT = (JSONArray) this.headerInfo.get(Table.JSON_KEY_COL_TYPES);
		for(int colCount= 0; colCount < columnCount; colCount++){
			aOutputStream.write(quote + jArrT.get(colCount) + quote);
			if(colCount != columnCount - 1){
				// we need a comma
				aOutputStream.write(",");
			}
		}
		aOutputStream.write("],");
		
		// write the row info
		aOutputStream.write(quote + Table.JSON_KEY_ROWS + quote + " : [");
				
		// done with metadata. flush.
		aOutputStream.flush();
		
		// process the data file rows until the cutoff is reached.
		int processedRows = 0;
		
		FileReader fr = new FileReader(dataFile);
		BufferedReader bfr = new BufferedReader(fr);
		
		// fast foward
		bfr = this.fastForwardResultsFile(bfr);
				
		while(processedRows < stopRowNumber){
			// read the next row from the data set and write to the stream. 
			
			String currRow = bfr.readLine();
			// conversion should not be required in this case as it was read as written.
			aOutputStream.write(currRow);
			
			// add the comma, if needed.
			if(processedRows < stopRowNumber -1){  aOutputStream.write(","); }
			
			processedRows += 1;
			
			if(processedRows % FLUSHFREQUENCY == 0){
				aOutputStream.flush();
				LocalLogger.logToStdErr("flushing after row: " +  processedRows);
			}
		}
		bfr.close();
		
		aOutputStream.write("]}");
		// done with rows. flush.
		aOutputStream.flush();
		LocalLogger.logToStdErr("flushing after completion: " +  processedRows);
		
	}
	
	private BufferedReader fastForwardResultsFile(BufferedReader bfr) throws IOException{
		
		for(int i = 0; i < this.startingRowNumber; i += 1){
			bfr.readLine();
		}
		return bfr;
	}
}
