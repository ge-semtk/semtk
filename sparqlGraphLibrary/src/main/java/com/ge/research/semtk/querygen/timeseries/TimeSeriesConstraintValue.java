/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.querygen.timeseries;


import com.ge.research.semtk.querygen.EdcConstraintValue;

public class TimeSeriesConstraintValue extends EdcConstraintValue{

	public TimeSeriesConstraintValue(String value, String type) throws Exception{
		super(value, type);
	}
	
	// get the internal values.
	public String getValue(){
		return this.val;
	}
	public String getType(){
		return this.type;
	}

	@Override
	public void setAllowedTypes() {
		String[] at = {"NUMBER", "STRING", "COLUMN", "DATETIME"};
		this.allowedTypes = at;		
	}
}

