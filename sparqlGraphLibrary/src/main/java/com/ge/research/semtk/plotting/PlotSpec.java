/**
 ** Copyright 2021 General Electric Company
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
package com.ge.research.semtk.plotting;

import org.json.simple.JSONObject;

/**
 * Information for a single plot spec, including name and type
 * 
 * Example:
 * 		{   type: "plotly", (e.g. plotly, visjs)
 *     		name: "line 1",
 *     		spec: { spec specific to the type }    
 * 		}
 */
@SuppressWarnings("unchecked")
public abstract class PlotSpec {
	
	public static final String JKEY_TYPE = "type";  // e.g. "plotly"
	public static final String JKEY_NAME = "name";  // display name
	public static final String JKEY_SPEC = "spec";

	JSONObject json = null;
	
	public PlotSpec(JSONObject json) throws Exception {
		
		// validate
		if(json == null){ throw new Exception("Cannot create PlotSpec with null JSON"); }
		if(json.get(JKEY_NAME) == null){ throw new Exception("Cannot create PlotSpec without a name"); }
		if(json.get(JKEY_TYPE) == null){ throw new Exception("Cannot create PlotSpec without a type"); }	
		if(json.get(JKEY_SPEC) == null){ throw new Exception("Cannot create PlotSpec without a spec"); }

		this.json = json;
	}
	
	public JSONObject toJson(){
		return json;
	}
	
	public String getType() throws Exception{
		if(!this.json.containsKey(JKEY_TYPE)){
			throw new Exception("Plot spec has no type");
		}
		return (String) this.json.get(JKEY_TYPE);
	}
	
	public String getName() throws Exception{
		if(!this.json.containsKey(JKEY_NAME)){
			throw new Exception("Plot spec has no name");
		}
		return (String) this.json.get(JKEY_NAME);
	}
	
	public JSONObject getSpec() throws Exception{
		if(!this.json.containsKey(JKEY_SPEC)){
			throw new Exception("Plot spec has no spec");
		}
		return (JSONObject) this.json.get(JKEY_SPEC);
	}
	
	public void setName(String name){
		this.json.remove(JKEY_NAME);
		this.json.put(JKEY_NAME, name);
	}
	
	public void setSpec(JSONObject specJson){
		this.json.remove(JKEY_SPEC);
		this.json.put(JKEY_SPEC, specJson);
	}
}
