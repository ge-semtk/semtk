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

public class ReplaceAllTransform extends Transform {

	private final static String name = "replaceAll";
	private String findString;
	private String replaceString;

	public ReplaceAllTransform(String InstanceName) {
		super(InstanceName);	
	}
	
	public ReplaceAllTransform(String instanceName, HashMap<String, String> args){
		super(instanceName, args);
	}
	
	@Override
	public String applyTransform(String input) {
		String retval = input.replaceAll(findString, replaceString);
		return retval;
	}
	
	@Override
	protected void fromArrayListOfArgs(HashMap<String, String> args) {
		// the JSON specifies the arguments to use
		this.findString = args.get("arg1");
		this.replaceString = args.get("arg2");
	}

	@Override
	public HashMap<String, String> getSpec() {
		// TODO Auto-generated method stub
		return null;
	}

}
