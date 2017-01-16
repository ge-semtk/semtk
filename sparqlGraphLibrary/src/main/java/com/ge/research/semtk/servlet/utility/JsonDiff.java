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


package com.ge.research.semtk.servlet.utility;

import java.util.HashMap;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonDiff {
	
	public JSONObject left = null;
	public JSONObject right = null;
	

	public JsonDiff(JSONObject left, JSONObject right){
		this.left = left;
		this.right = right;
	}
	public Boolean leftMatchesRightRecursive(JSONObject left, JSONObject right, String path){
		Set<String> leftKeys = (Set<String>)left.keySet();
		Set<String> rightKeys = (Set<String>)right.keySet();
		Boolean retval = true;
		HashMap<String, Integer> commonKeys = new HashMap<String, Integer>();
		
		System.out.println("for the path: " + path);
		for(String lk : leftKeys){
			if(!right.containsKey(lk)){
				// no match.
				retval = false;
				System.out.println("right did not contain key: " + lk);
			}
			else{
				commonKeys.put(lk, 1);
			}
		}
		
		for(String rk : rightKeys) {
			if(!left.containsKey(rk)){
				// no match
				retval = false;
				System.out.println("left did not contain key: " + rk);
			}
			
		}

		// check all the values of left to make sure it matches right.
		
		for(String lk : commonKeys.keySet()){
				
			try{
				JSONObject lVal = (JSONObject) left.get(lk);
			
				JSONObject rVal = (JSONObject) right.get(lk);
				// check if they match.
				if(!lVal.toJSONString().equalsIgnoreCase(rVal.toJSONString())){
					// right and left disagree.
					System.out.println("left and right mismatch for key :" + lk);
					System.out.println("LEFT  = " + lVal.toJSONString());
					System.out.println("RIGHT = " + rVal.toJSONString());
					
					this.leftMatchesRightRecursive(lVal, rVal, path + "-->" + lk);
					
					retval = false;
				}
			}
			catch(Exception eee){
				JSONArray lVal = (JSONArray)  left.get(lk);
				JSONArray rVal = (JSONArray)  right.get(lk);
				
				// check for matching. 
				int sizeCheck = 0;
				if(lVal.size() < rVal.size()){ sizeCheck = lVal.size(); }
				else{ sizeCheck = rVal.size(); }
				
				Boolean seenit = false;
				
				// check?
				for(int i = 0; i < sizeCheck; i += 1){
					if(!lVal.get(i).toString().equalsIgnoreCase(rVal.get(i).toString())){
						if(!seenit){
							System.out.println("left and right mismatch for key :" + lk);
							seenit = true;
						}
						System.out.print("LEFT  = " + lVal.get(i));
						System.out.println("   || RIGHT = " + rVal.get(i));
						retval = false;
					}
					
				
				}
				// rest 
				if(lVal.size() > rVal.size()){
					for(int i = sizeCheck; i < lVal.size(); i += 1){
						System.out.print("LEFT  = " + lVal.get(i));
						System.out.println("   || RIGHT = [null]");
					}
					
				}
				else if(lVal.size() < rVal.size()){
					for(int i = sizeCheck; i < rVal.size(); i += 1){
						System.out.print("LEFT  = [null]" );
						System.out.println("   || RIGHT = " + rVal.get(i));
					}	
					
				}
			}
			
		}
		
		System.out.println("End path " + path);
		return retval;
	}
	public Boolean leftMatchesRight(){
		Set<String> leftKeys = (Set<String>)this.left.keySet();
		Set<String> rightKeys = (Set<String>)this.right.keySet();
		Boolean retval = true;
		
		HashMap<String, Integer> commonKeys = new HashMap<String, Integer>();
		
		// check all the keys in left to make sure they appear in right.
		for(String lk : leftKeys){
			if(!this.right.containsKey(lk)){
				// no match.
				retval = false;
				System.out.println("right did not contain key: " + lk);
			}
			else{
				commonKeys.put(lk, 1);
			}
		}
		
		for(String rk : rightKeys) {
			if(!this.left.containsKey(rk)){
				// no match
				retval = false;
				System.out.println("left did not contain key: " + rk);
			}
			
		}
		// check all the values of left to make sure it matches right.
		
		for(String lk : commonKeys.keySet()){
				JSONObject lVal = (JSONObject) this.left.get(lk);
				JSONObject rVal = (JSONObject) this.right.get(lk);
				// check if they match.
				if(!lVal.toJSONString().equalsIgnoreCase(rVal.toJSONString())){
					// right and left disagree.
					System.out.println("left and right mismatch for key :" + lk);
					System.out.println("LEFT  = " + lVal.toJSONString());
					System.out.println("RIGHT = " + rVal.toJSONString());
					retval = false;
				}
		
			
		}
		
		return retval;
	}
	
}
