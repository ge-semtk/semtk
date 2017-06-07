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

package com.ge.research.semtk.belmont;


public class XSDSupportUtil {

	private static String xmlSchemaPrefix = "^^<http://www.w3.org/2001/XMLSchema#";
	private static String xmlSchemaTrailer = ">";
			
	public static boolean supportedType(String candidate){
		boolean retval = true;
		
		try{
			XSDSupportedTypes.valueOf(candidate.toUpperCase());
			retval = true;
		}
		catch(IllegalArgumentException e){
			// the specified value is not a valid key name in the enum.
			retval = false;
		}
		return retval;
	}
	
	public static String getXsdSparqlTrailer(String candidate) throws Exception{
		String retval = xmlSchemaPrefix + candidate.toLowerCase() + xmlSchemaTrailer;
		
		try{
			XSDSupportedTypes.valueOf(candidate.toUpperCase());
		}
		catch(IllegalArgumentException e){
			throw new Exception("unrecognized type: " + candidate + ". does not match XSD types defined in " +  XSDSupportedTypes.class.getName().toLowerCase()); 
		}
		return retval;
	}
	
	public static boolean regexIsAvailable(String candidate){
		boolean retval = false;
		
		if(supportedType(candidate)){
			// this is a type we understand. we can now check.
			// right now, it is probably safe to assume that only strings can be regexed.
			
			try{
				if(XSDSupportedTypes.STRING == XSDSupportedTypes.valueOf(candidate.toUpperCase())){
					retval = true;
				}
			
			}
			catch(Exception eee){
				// do nothing.
			}
		}

		return retval;
	}
	
	public static boolean booleanOperationAvailable(String candidate){
		boolean retval = false;
		
		if(supportedType(candidate)){
			// this is a type we understand. we can now check.
			
			try{
				if(XSDSupportedTypes.BOOLEAN == XSDSupportedTypes.valueOf(candidate.toUpperCase())){
					retval = true;
				}
			
			}
			catch(Exception eee){
				// do nothing.
			}
		}

		return retval;
	}	
	public static boolean dateOperationAvailable(String candidate){
		boolean retval = false;
		boolean passedInitialCheck = true;
				
		if(passedInitialCheck && supportedType(candidate)){
			// this is a type we understand. we can now check.
			// right now, it is probably safe to assume that only strings can be regexed.
			
			try{
				// assuming that these are valid:
				//  DATETIME , TIME , DATE
				
				if(	
						XSDSupportedTypes.DATETIME == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.DATE  == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.TIME == XSDSupportedTypes.valueOf(candidate.toUpperCase()) 
					){
					retval = true;
				}
			
			}
			catch(Exception eee){
				// do nothing.
			}
		}

		return retval;		
	}
	
	public static boolean numericOperationAvailable(String candidate){
		boolean retval = false;
		
		if(supportedType(candidate)){
			// this is a type we understand. we can now check.
			// right now, it is probably safe to assume that only strings can be regexed.
			
			try{
				// assuming that these are valid:
				//  DECIMAL , INT , INTEGER , NEGATIVEINTEGER , NONNEGATIVEINTEGER , 
				//  POSITIVEINTEGER, NONPOSISITIVEINTEGER , LONG , FLOAT , DOUBLE
				
				if(	
						XSDSupportedTypes.INT == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.DECIMAL == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.INTEGER == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.NEGATIVEINTEGER == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.NONNEGATIVEINTEGER == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.POSITIVEINTEGER == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.NONPOSISITIVEINTEGER == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.LONG == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.FLOAT == XSDSupportedTypes.valueOf(candidate.toUpperCase()) ||
						XSDSupportedTypes.DOUBLE == XSDSupportedTypes.valueOf(candidate.toUpperCase())
					){
					retval = true;
				}
			
			}
			catch(Exception eee){
				// do nothing.
			}
		}

		return retval;		
	}
	
	
}
