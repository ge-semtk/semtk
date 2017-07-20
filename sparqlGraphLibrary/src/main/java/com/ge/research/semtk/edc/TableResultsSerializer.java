package com.ge.research.semtk.edc;

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

import com.ge.research.semtk.edc.TableResultsStorage.TableResultsStorageTypes;
import com.ge.research.semtk.resultSet.Table;

public class TableResultsSerializer {
	
	private static final int FLUSHFREQUENCY = 100;
	private JSONObject headerInfo = null;
	private File dataFile = null;
	private TableResultsStorageTypes frmt = null;
	private Integer cutoffValue = null;
	
	public TableResultsSerializer(){
		
	}
	public TableResultsSerializer(JSONObject headerInfo, String dataFileLocation, TableResultsStorageTypes serializationType, Integer cutoff){
		 
		this.headerInfo = headerInfo;
		this.dataFile = new File(dataFileLocation);
		this.frmt = serializationType;
		this.cutoffValue = cutoff;
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
			this.cutoffValue = totalRowsExpected;
		}
		else if(this.cutoffValue == null){ this.cutoffValue = totalRowsExpected; }
		
		
		System.err.println("requested file record size = " + cutoffValue);
		
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
		while(processedRows < stopRowNumber && !endOfInput){
			// read the next row from the data set and write to the stream. 
			
			String currRow = bfr.readLine();
			// conversion should not be required in this case as it was read as written.
			aOutputStream.write(currRow.substring(1, currRow.length() - 1));
		
			// add the comma, if needed.
			if(processedRows < stopRowNumber -1){  aOutputStream.write("\n"); }
			
			processedRows += 1;
			
			if(processedRows % FLUSHFREQUENCY == 0){
				System.err.println("flushing after row: " +  processedRows);
				aOutputStream.flush();
			}
		}
		
		// done with rows. flush.
		aOutputStream.flush();
		System.err.println("flushing after completion: " +  processedRows);
	}
	
	private void writeJSON( PrintWriter aOutputStream, Integer stopRowNumber ) throws UnsupportedOperationException, IOException{
		boolean done = false;
	
		if(this.headerInfo == null){ throw new UnsupportedOperationException("cannot return info when metadata is empty or nonexistent"); }
		// open the data file
		if(!this.dataFile.exists()){ throw new UnsupportedOperationException("cannot return info when data file is nonexistent"); }
		
		// write the metadata to the stream
		int columnCount = Integer.parseInt( "" + this.headerInfo.get(Table.JSON_KEY_COL_COUNT));
		
		System.err.println("requested row count : " + stopRowNumber);
		
		aOutputStream.write("{\"" + Table.JSON_KEY_ROW_COUNT + "\" : "  + stopRowNumber + ",");
		aOutputStream.write("\"" + Table.JSON_KEY_COL_COUNT + "\" : "  + columnCount + ",");
		aOutputStream.write("\"" + Table.JSON_KEY_COL_NAMES + "\" : [");
		
		JSONArray jArr = (JSONArray) this.headerInfo.get(Table.JSON_KEY_COL_NAMES);
		for(int colCount= 0; colCount < columnCount; colCount++){
			aOutputStream.write("\"" + jArr.get(colCount) + "\"");
			if(colCount != columnCount - 1){
				// we need a comma
				aOutputStream.write(",");
			}
		}
		aOutputStream.write("],");
		
		aOutputStream.write("\"" + Table.JSON_KEY_COL_TYPES + "\" : [");
		JSONArray jArrT = (JSONArray) this.headerInfo.get(Table.JSON_KEY_COL_TYPES);
		for(int colCount= 0; colCount < columnCount; colCount++){
			aOutputStream.write("\"" + jArrT.get(colCount) + "\"");
			if(colCount != columnCount - 1){
				// we need a comma
				aOutputStream.write(",");
			}
		}
		aOutputStream.write("],");
		
		// write the row info
		aOutputStream.write("\"" + Table.JSON_KEY_ROWS + "\" : [");
				
		// done with metadata. flush.
		aOutputStream.flush();
		
		// process the data file rows until the cutoff is reached.
		int processedRows = 0;
		
		FileReader fr = new FileReader(dataFile);
		BufferedReader bfr = new BufferedReader(fr);
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
				System.err.println("flushing after row: " +  processedRows);
			}
		}
		
		aOutputStream.write("]}");
		// done with rows. flush.
		aOutputStream.flush();
		System.err.println("flushing after completion: " +  processedRows);
		
	}
}
