package com.ge.research.semtk.plotting;

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
	private static String JKEY_TYPE = "type";
	
	JSONArray json = null;

	public PlotsHandler(JSONArray json) {
		super();
		this.json = json;
	}
	
	public int getNumPlots() {
		if (json == null) {
			return 0;
		} else {
			return this.json.size();
		}
	}
	
	public PlotlyPlotSpecHandler getPlot(int index) throws Exception {
		JSONObject plot = (JSONObject) this.json.get(index);
		String t = (String) plot.get(JKEY_TYPE);
		if (t == null) 
			throw new Exception("Plot spec is missing type");
		
		switch ((String) plot.get(JKEY_TYPE)) {
			case "plotly":
				return new PlotlyPlotSpecHandler(plot);
			default:
				throw new Exception("Unknown plot type: " + t);
		}
				
	}
	
}
