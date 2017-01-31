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

package com.ge.research.semtk.utility;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class Utility {

	
	/**
	 * Retrieve a property from a properties file.
	 * @param propertyFile the property file
	 * @param key the name of the property to retrieve
	 */
	public static String getPropertyFromFile(String propertyFile, String key) throws Exception{
	
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(new File(propertyFile)));
		} catch (Exception e) {
		    throw new Exception("Cannot load properties file " + propertyFile, e);
		}
		// now read the property		
		String ret = properties.getProperty(key);
		if(ret == null){
		    throw new Exception("Cannot read property '" + key + "' from " + propertyFile);	
		}
		
		return ret.trim();
	}
	
}
