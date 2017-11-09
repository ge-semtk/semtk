package com.ge.research.semtk.edc.resultsStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.ge.research.semtk.utility.LocalLogger;

public class GenericJsonBlobResultsSerializer {

	private static final int FLUSHFREQUENCY = 100;
	private File dataFile = null;
	
	public GenericJsonBlobResultsSerializer(String dataFileLocation){
		this.dataFile = new File(dataFileLocation);
	}
	
	public void writeToStream(PrintWriter printWriter ) throws IOException, UnsupportedOperationException {
		this.writeJSON(printWriter);
	}
	
	private void writeJSON( PrintWriter aOutputStream ) throws UnsupportedOperationException, IOException{
		
		String quote = "\"";
		
		// open the data file
		if(!this.dataFile.exists()){ throw new UnsupportedOperationException("cannot return info when data file is nonexistent"); }
		
		// write the metadata to the stream
		
		// process the data file rows until the cutoff is reached.
		int processedRows = 0;
		boolean endOfInput = false;
		
		FileReader fr = new FileReader(dataFile);
		BufferedReader bfr = new BufferedReader(fr);
		
		while(!endOfInput){
			// read the next row from the data set and write to the stream. 

			String currRow = bfr.readLine();
			
			// stop on end of input
			if(currRow == null){ break; }
			
			// conversion should not be required in this case as it was read as written.
			aOutputStream.write(currRow);
			
			processedRows += 1;
			
			if(processedRows % FLUSHFREQUENCY == 0){
				LocalLogger.logToStdErr("flushing after row: " +  processedRows);
				aOutputStream.flush();
			}
		}
		
		// done with rows. flush.
		aOutputStream.flush();
		LocalLogger.logToStdErr("flushing after completion: " +  processedRows);
	}
}
