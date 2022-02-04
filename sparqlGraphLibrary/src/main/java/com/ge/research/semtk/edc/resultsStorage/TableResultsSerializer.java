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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.StringEscapeUtils;
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
		
		if(this.frmt.equals(TableResultsStorageTypes.JSON)){
			this.writeJSON(aOutputStream, cutoffValue);
		}
		else if(this.frmt.equals(TableResultsStorageTypes.CSV)){
			this.writeCSV(aOutputStream, cutoffValue);
		}
		else{
			throw new UnsupportedOperationException("TableResultsSerializer.serializationFormat." + this.frmt.name() + " is not supported for serialization. version mismatch?");
		}
    }
	
	/**
	 * Reads the stored JSON and translates it into a CSV file
	 * @param aOutputStream
	 * @param stopRowNumber
	 * @throws UnsupportedOperationException
	 * @throws IOException
	 */
	private void writeCSV( PrintWriter aOutputStream, Integer stopRowNumber ) throws UnsupportedOperationException, IOException{
		
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
		
		FileInputStream fis = new FileInputStream(dataFile.getPath());
		InputStreamReader isr = new InputStreamReader(fis,"utf-8");
		BufferedReader bfr = new BufferedReader(isr);
		
		// fast foward
		bfr = this.fastForwardResultsFile(bfr);
		
		while(processedRows < stopRowNumber && !endOfInput){
			// read the next row from the data set and write to the stream. 

			String currRow = bfr.readLine();
			
			// stored json version has quotes around each field and \" for embedded quotes
			// like this ["1", "ab", "I \"love\" cookies"]
			// remove brackets from the json line
			currRow = currRow.substring(1, currRow.length() - 1);
			// change \" to "" so that CSVParser will work
			currRow = currRow.replaceAll("\\\\\\\"",  "\"\"");
			
			CSVParser parser = CSVParser.parse(currRow, CSVFormat.EXCEL);
			StringBuffer line = new StringBuffer();
			CSVPrinter printer = new CSVPrinter(line, CSVFormat.EXCEL);
			
			// pull apart the string by CSV field using all the proper quoting and , rules
			for (CSVRecord r : parser.getRecords()) {
				for (int i=0; i < r.size(); i++) {
					// unescape the JSON - e.g. changes \n to line return
					// then re-assemble the line using the proper quoting and , rules
					printer.print(StringEscapeUtils.unescapeJson(r.get(i)));
				}
			}
			
			aOutputStream.write(line.toString());
			
			// add the comma, if needed.
			if(processedRows < stopRowNumber -1){  aOutputStream.write("\n"); }
			
			processedRows += 1;
			
			if(processedRows % FLUSHFREQUENCY == 0){
				LocalLogger.logToStdOut("writeCSV finished row " +  processedRows + " of " + stopRowNumber);
				aOutputStream.flush();
			}
		}
		bfr.close();
		
		// done with rows. flush.
		aOutputStream.flush();
		LocalLogger.logToStdOut("writeCSV finished after " +  processedRows + " rows");
	}
	
	private void writeJSON( PrintWriter aOutputStream, Integer stopRowNumber) throws UnsupportedOperationException, IOException{

		String quote = "\"";
		
		if(this.headerInfo == null){ throw new UnsupportedOperationException("cannot return info when metadata is empty or nonexistent"); }
		// open the data file
		if(!this.dataFile.exists()){ throw new UnsupportedOperationException("cannot return info when data file is nonexistent"); }
		
		// write the metadata to the stream
		int columnCount = Integer.parseInt( "" + this.headerInfo.get(Table.JSON_KEY_COL_COUNT));
		
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
		
		FileInputStream fis = new FileInputStream(dataFile.getAbsolutePath());
		InputStreamReader isr = new InputStreamReader(fis,"utf-8");
		BufferedReader bfr = new BufferedReader(isr);
				
		// fast foward
		bfr = this.fastForwardResultsFile(bfr);
		if(bfr == null){ throw new IOException("BufferedReader is null after fastForwardResults() for " + dataFile.getAbsolutePath()); }	// TODO troubleshooting NullPointerException - delete if fixed	 
		
		while(processedRows < stopRowNumber){
			// read the next row from the data set and write to the stream. 
			
			String currRow = bfr.readLine();
			aOutputStream.write(currRow); 											
			
			// add the comma, if needed.
			if(processedRows < stopRowNumber -1){  aOutputStream.write(","); }
			
			processedRows += 1;
			
			if(processedRows % FLUSHFREQUENCY == 0){
				aOutputStream.flush();
				LocalLogger.logToStdOut("writeJSON finished row " +  processedRows + " of " + stopRowNumber);
			}
		}
		bfr.close();
		
		aOutputStream.write("]}");
		// done with rows. flush.
		aOutputStream.flush();
		LocalLogger.logToStdOut("writeJSON finished after " +  processedRows + " rows");
		
	}
	
	private BufferedReader fastForwardResultsFile(BufferedReader bfr) throws IOException{
		
		for(int i = 0; i < this.startingRowNumber; i += 1){
			bfr.readLine();
		}
		return bfr;
	}
}
