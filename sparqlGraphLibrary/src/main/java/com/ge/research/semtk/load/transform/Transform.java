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

public abstract class Transform {

	private final static String name = "replace_in_implementations";
	private String localID;

	public Transform(String InstanceName){
		this.localID = InstanceName;
	}
	public Transform(String instanceName, HashMap<String, String> args){
		this(instanceName);
		this.fromArrayListOfArgs(args);
	}
	
	public abstract String applyTransform(String input);
	protected abstract void fromArrayListOfArgs(HashMap<String, String> args);
	public abstract HashMap<String, String> getSpec();
}
