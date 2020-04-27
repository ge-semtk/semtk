/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.query.rdb.test;


import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;



public class AthenaConnectorTest_IT {
	
	private static String ATHENA_DATABASE;	
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		ATHENA_DATABASE = IntegrationTestUtility.get("athena.database");

		// if needed variables are not configured, then skip test(s)
		Assume.assumeTrue("No Athena Service Test Database is configured in environment", ATHENA_DATABASE != null && !ATHENA_DATABASE.trim().isEmpty());
	}
	
	// TODO add test

}
