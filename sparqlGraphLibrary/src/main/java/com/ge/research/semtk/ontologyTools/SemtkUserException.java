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


package com.ge.research.semtk.ontologyTools;

/**
 * Generic SemTK exception:  message is intended for user.  
 *                           No stack tracing for developer is needed.
 * @author 200001934
 *
 */
public class SemtkUserException extends Exception {

	private static final long serialVersionUID = 2L;
	
	//Parameterless Constructor
	public SemtkUserException() {}
	
	//Constructor that accepts a message
	public SemtkUserException(String message)
	{
		super(message);
	}
}