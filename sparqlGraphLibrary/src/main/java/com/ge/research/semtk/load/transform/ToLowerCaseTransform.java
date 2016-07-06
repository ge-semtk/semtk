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


package com.ge.research.semtk.load.transform;

import java.util.HashMap;

import com.ge.research.semtk.load.transform.Transform;

public class ToLowerCaseTransform extends Transform {

	private final static String name = "toLowerCase";

	public ToLowerCaseTransform(String InstanceName) {
		super(InstanceName);	
	}
	
	public ToLowerCaseTransform(String instanceName, HashMap<String, String> args){
		super(instanceName, args);
	}
	
	@Override
	public String applyTransform(String input) {
		String retval = input.toLowerCase();
		return retval;
	}
	
	@Override
	protected void fromArrayListOfArgs(HashMap<String, String> args) {
		// the JSON specifies the arguments to use
	}

	@Override
	public HashMap<String, String> getSpec() {
		// TODO Auto-generated method stub
		return null;
	}

}
