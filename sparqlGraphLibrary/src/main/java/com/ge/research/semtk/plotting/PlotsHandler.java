package com.ge.research.semtk.plotting;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

/**    sparqlgraphjson might have:
    .plots: [
    		{ type: plotly,
	 	      name: "line 1",
              spec: { data: [{},{}], layout: {}, config: {} }    
            },
               { type: visjs,
                 spec: { vsJsSpecJson }
               },
               { type: plotly,
                 spec: { data, layout, config }
             },
	 */

public class PlotsHandler {
	
	private static String JKEY_NAME = "name";
	private static String JKEY_TYPE = "type";
	
	private JSONArray jsonArr = null;

	public PlotsHandler(JSONArray json) {
		super();
		this.jsonArr = json;
	}
	
	/**
	 * Get number of plots
	 */
	public int getNumPlots() {
		if (jsonArr == null) {
			return 0;
		} else {
			return this.jsonArr.size();
		}
	}
	
	/**
	 * Get plot names 
	 * @return an array of names (empty if no plot specs present)
	 * @throws Exception if there is a plot spec with no name key
	 */
	public String[] getPlotNames() throws Exception {
		ArrayList<String> namesList = new ArrayList<String>();
		if(jsonArr != null){
			for(Object o : jsonArr){
				JSONObject jsonObject = (JSONObject)o;
				String name = (String) jsonObject.get(JKEY_NAME);
				if(name == null) throw new Exception("Plot spec is missing 'name'");
				namesList.add(name);
			}
		}
		String[] ret = new String[namesList.size()];
		return namesList.toArray(ret);
	}
	
	/**
	 * Get handler for a given plot
	 */
	public PlotlyPlotSpecHandler getPlot(int index) throws Exception {
		JSONObject plot = (JSONObject) this.jsonArr.get(index);
		String t = (String) plot.get(JKEY_TYPE);
		if (t == null) 
			throw new Exception("Plot spec is missing 'type'");
		
		switch ((String) plot.get(JKEY_TYPE)) {
			case "plotly":
				return new PlotlyPlotSpecHandler(plot);
			default:
				throw new Exception("Unknown plot type: " + t);
		}
				
	}
	
}
