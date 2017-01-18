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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import com.ge.research.semtk.load.transform.Transform;

public class TransformInfo {
	private static final Map<String, String[]> transformList;
	static{ // add to this when creating additional entries for the transform types
		 	// the order of items in the string array are: argumentCount, actual transform class name
		HashMap<String, String[]> tMap = new HashMap<String, String[]>();
		
		tMap.put("replaceAll", new String[]{"2", ReplaceAllTransform.class.getCanonicalName()});
		tMap.put("toUpperCase", new String[]{"0", ToUpperCaseTransform.class.getCanonicalName()});
		tMap.put("toLowerCase", new String[]{"0", ToLowerCaseTransform.class.getCanonicalName()});
		tMap.put("hashCode", new String[]{"0", HashTransform.class.getCanonicalName()});
		
		// add to the static mapping. 
		transformList = tMap; // set it.
	}
	
	public static int getArgCount(String transformName) throws Exception{
		try{
			String[] desired = transformList.get(transformName);
			int retval = Integer.parseInt(desired[0]);
			return retval;
		}
		catch(Exception Eee){
			throw new Exception("Transform " + transformName + " not found or no argument count provided.");
		}
	}
	/**
	 * construct a specific transform subtype by consulting the registry of known types and then 
	 * return it after casting to a generic transform.
	 * @param transformName
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public static Transform buildTransform(String transformName, String instanceName, HashMap<String, String> args) throws Exception{
		try{
			// use the Transform name to get the related class. 
			// use the class to get the constructor
			// use the constructor to get the instance. 
			Object retval = null;
			String[] desired = transformList.get(transformName);
			Class selectedTransform = Class.forName( desired[1]);		
			Constructor selectedTransformConstructor = selectedTransform.getConstructor(String.class, HashMap.class);
			retval = selectedTransformConstructor.newInstance(instanceName, args);
			
			return (Transform)retval; // cast to generic transform and return
		}
		catch(Exception E){
			String exposedTransforms = "";
			for(String K : transformList.keySet()){
				exposedTransforms += K + "(" + (transformList.get(K))[1] + ") , ";
			}
			
			throw new Exception("Transform " + transformName + " not found or no argument count provided. exposed Transforms are " + exposedTransforms);			
		}
	}
	
}
