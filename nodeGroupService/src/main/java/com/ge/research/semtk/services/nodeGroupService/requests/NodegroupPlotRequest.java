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

package com.ge.research.semtk.services.nodeGroupService.requests;

import io.swagger.annotations.ApiModelProperty;

public class NodegroupPlotRequest extends NodegroupRequest {

	@ApiModelProperty(position = 1,  value = "plotName", required = true, example = "Sample Plot")
    private String plotName;

	@ApiModelProperty(position = 1,  value = "plotType", required = true, example = "plotly")
    private String plotType;
	
	@ApiModelProperty(position = 1,  value = "graphType", required = true, example = "scatter")
    private String graphType;
	
	@ApiModelProperty(value = "columnNames[]", required = true, example = "[colA, colB]")	
	private String[] columnNames;
	
	public String getPlotName() {
		return this.plotName;
	}
	
	public String getPlotType() {
		return this.plotType;
	}
	
	public String getGraphType() {
		return this.graphType;
	}
	
	public String[] getColumnNames(){
		return columnNames;
	}
}