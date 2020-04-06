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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;



/*
 * Utility methods
 */
public abstract class Utility {

	/*
	 * Convert dates.
	 * Assume incoming date is of format 7/27/2009 or 7/27/2009 7:55.
	 * Convert 7/27/2009 		to 2009-07-27
	 * Convert 7/27/2009 7:55 	to 2009-07-27 07:55:00
	 */
	public static String convertToHiveDate (String str) throws ParseException {

		if(str.length() <= 10){
			// Convert 7/27/2009 to 2009-07-27
			Date date = (new SimpleDateFormat("MM/dd/yyyy")).parse(str);
			return new SimpleDateFormat("yyyy-MM-dd").format(date);
		}else{
			// Convert 7/27/2009 7:55 to 2009-07-27 07:55:00
			Date date = (new SimpleDateFormat("MM/dd/yyyy hh:mm")).parse(str);
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		}
	}


	/**
	 * Format date from MM/dd/yyyy to yyyy-MM-dd
	 * @throws Exception
	 */
	public static String formatToSemanticStoreDate (String str) throws Exception {
		try {
			SimpleDateFormat formatInput = new SimpleDateFormat("MM/dd/yyyy");
			formatInput.setLenient(false);
			Date date = formatInput.parse(str);
			SimpleDateFormat formatOutput = new SimpleDateFormat("yyyy-MM-dd");
			return formatOutput.format(date);
		}
		catch(Exception e) {
			throw new Exception("Cannot parse/format date: " + str);
		}
	}

	/**
	 * Get today's date as yyyy-MM-dd
	 * @return
	 */
	public static String getTodaysDateInSemanticStoreFormat(){
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(date);
	}

	/**
	 * Merge an array of Strings into a single String.
	 */
	public static String mergeArray (String[] array) {
		String tmp = "";
		for (int i = 0; i < array.length; i++) {
			if (i != 0){
				tmp += ",";
			}
			if ((array[i] != null) && (!array[i].equalsIgnoreCase("null"))){
				tmp += array[i].trim();
			}
		}
		return tmp;
	}

	/**
	 * Find the index of the nth occurrence of a character within a String
	 */
	public static int nthIndexOf(String text, char c, int n)
	{
		for (int i = 0; i < text.length(); i++)	    {
			if (text.charAt(i) == c)	        {
				n--;
				if (n == 0)	            {
					return i;
				}
			}
		}
		return -1;
	}


	/**
	 * Run a system command.  Returns true if error code is 0, else false.
	 * @param command
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean runSystemCommand(String command) throws IOException, InterruptedException{
		System.out.println("Run system command: " + command);
		String s = null;
		try {

			// using the Runtime exec method:
			Process p = Runtime.getRuntime().exec(command);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			// read the output from the command
			System.out.println("...standard output:");
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}

			// read any errors from the attempted command
			System.out.println("...standard error:");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}

			p.waitFor();  	// TODO is this OK/necessary?
			System.out.println("EXIT VALUE = " + p.exitValue());
			int exitValue = p.exitValue();
			if(exitValue == 0){
				return true; // success
			}else{
				return false;
			}

		}
		catch (IOException e) {
			System.out.println("Exception running system command:");
			e.printStackTrace();
			throw e;
		}
	}

}
