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


package com.ge.research.semtk.auth;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ge.research.semtk.utility.LocalLogger;

public class AuthorizationException extends Exception {
	static final long serialVersionUID = 23098467;
	private static String authLogPath = null;
	
	
	public AuthorizationException(String message) {
		super(message);
		logStackTrace(this);
	}
	public AuthorizationException(String message, Exception e) {
		super(message, e);
		logStackTrace(this);
	}
	
	
	public static void setAuthLogPath(String authLogPath) {
		AuthorizationException.authLogPath = authLogPath;
	}
	
	/**
	 * Log authorization events to a separate log if specified and possible
	 * @param msg
	 */
	public static void logAuthEvent(String msg) {
		String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		
		if (authLogPath != null && !authLogPath.isEmpty()) {
			String fullMsg = dateStr + " " + msg;
			try {
				Path p = Paths.get(authLogPath);
				Files.write(p, fullMsg.getBytes(), Files.exists(p) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
				return;
			} catch (Exception e) {
			}
		} else {
			LocalLogger.logToStdErr("AuthorizationException: " + msg);
		}
		
	}
	
	/**
	 * Log authorizationException if there's a separate log
	 * @param msg
	 */
	public static void logStackTrace(AuthorizationException ae) {
		String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		
		if (authLogPath != null && !authLogPath.isEmpty()) {
			
			PrintStream ps = null;
			try {
				File f = Paths.get(authLogPath).toFile();
				ps = new PrintStream(f);
				ae.printStackTrace(ps);

				
			} catch (Exception e) {
			} finally {
				try {
					ps.close();
				} catch (Exception ee) {}
			}
		} else {
			LocalLogger.printStackTrace(ae);
		}
		
	}
}
