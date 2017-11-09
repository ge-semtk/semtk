package com.ge.research.semtk.edc.resultsStorage;

import java.net.URL;

import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

public class GenericJsonBlobResultsStorage extends GeneralResultsStorage {

	public GenericJsonBlobResultsStorage(String fileLocation) {
		super(fileLocation);
	}

	public URL storeResults(String jobID, String jsonData) throws Exception{
		String dataFileName = writeToFile(jobID, null, true);
		
		JSONObject jsonMetaData = new JSONObject();
		// record the meta data on where the real result is.
		jsonMetaData.put(DATARESULTSFILELOCATION, dataFileName);
		
		String metadatafile = writeToFile(jobID, jsonMetaData.toJSONString(), false);
		// create and write the results data file
		String fileName = writeToFile(jobID, jsonData, true);
		
		LocalLogger.logToStdErr("Blob metadata file was: " + metadatafile);
		LocalLogger.logToStdErr("Blob data file was: " + fileName);
		
		return getURL(metadatafile);
	}
	
	public GenericJsonBlobResultsSerializer getJsonBlob(URL url) throws Exception{
		try{
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath(urlToPath(url).toString());	// read json from url
			
			String dataFileLocation = (String) jsonObj.get(DATARESULTSFILELOCATION);
			
			return new GenericJsonBlobResultsSerializer(dataFileLocation);

		}catch(Exception e){
			LocalLogger.printStackTrace(e);
			throw new Exception("Could not read results from store for " + url + ": " + e.toString());
		}
	}
	
	
}
