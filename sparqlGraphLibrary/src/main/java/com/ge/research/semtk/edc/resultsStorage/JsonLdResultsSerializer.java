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
import java.io.PrintWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;

public class JsonLdResultsSerializer {
	private static final int FLUSHFREQUENCY = 100;
	private File dataFile = null;

	public JsonLdResultsSerializer(String dataFileLocation){
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
