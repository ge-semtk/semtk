/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.standaloneExecutables;

import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;

/**
 * Loads OWL to a semantic store.
 */
public class StoreNodeGroup {
	
	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception{
		
		try{
		
			// get arguments
			if(args.length != 5) {
				throw new Exception("Usage: http://endpoint:port nodegroupID \"some comments\" owner file.json");
			}
			
			String endpointUrlWithPort = args[0];
			String ngId = args[1];	
			String ngComments = args[2];	
			String ngOwner = args[3];	
			String ngFilePath = args[4];	
			String endpointPart [] = endpointUrlWithPort.split(":/*");
			
			// validate arguments
			if(!ngFilePath.endsWith(".json")){
				throw new Exception("Error: Connection file " + ngFilePath + " is not a JSON file");
			}
			JSONObject ngJson = Utility.getJSONObjectFromFilePath(ngFilePath);
			
			// set up client
			NodeGroupStoreConfig config = new NodeGroupStoreConfig(endpointPart[0], endpointPart[1], Integer.parseInt(endpointPart[2]));
			NodeGroupStoreRestClient client = new NodeGroupStoreRestClient(config);
			
			// check whether id already exists
			TableResultSet res = client.executeGetNodeGroupMetadata();
			res.throwExceptionIfUnsuccessful("Error checking of nodegroup Id already exists");
			
			// delete old copy if it does
			if (ArrayUtils.contains(res.getTable().getColumn("ID"), ngId)) {
				
				SimpleResultSet r = client.deleteStoredNodeGroup(ngId);
				r.throwExceptionIfUnsuccessful("Error removing preview version of nodegroup");
			}
			
			// store it
			SimpleResultSet r = client.executeStoreNodeGroup(ngId, ngComments, ngOwner, ngJson);
			r.throwExceptionIfUnsuccessful("Error storing nodegroup");
			
			LocalLogger.logToStdOut("Successfully stored " + ngId);
		
		} catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}
		
		System.exit(0);
	}
		
}
