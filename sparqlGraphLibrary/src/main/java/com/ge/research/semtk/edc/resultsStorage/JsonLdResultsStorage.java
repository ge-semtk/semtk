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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.Utility;

public class JsonLdResultsStorage extends GeneralResultsStorage {
	
	public JsonLdResultsStorage(String fileLocation){
		super(fileLocation);
	}
	
	public URL storeResults(String jobID, JSONObject jsonLdMetaData, String jsonLdData) throws Exception{
		String dataFileName = writeToFile(jobID, null, true);
		
		// record the meta data on where the real result is.
		jsonLdMetaData.put(DATARESULTSFILELOCATION, dataFileName);
		
		// create and write the results metadata file
		writeToFile(jobID, jsonLdMetaData.toJSONString(), false);
		// create and write the results data file
		String fileName = writeToFile(jobID, jsonLdData, true);
		
		
		return getURL(fileName);
	}
	
	public JsonLdResultsSerializer getJsonLd(URL url) throws Exception{
		
		try{
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath(urlToPath(url).toString());	// read json from url
			
			String dataFileLocation = (String) jsonObj.get(DATARESULTSFILELOCATION);
			
			return new JsonLdResultsSerializer(dataFileLocation);

		}catch(Exception e){
			e.printStackTrace();
			throw new Exception("Could not read results from store for " + url + ": " + e.toString());
		}
		
	}
	
}
