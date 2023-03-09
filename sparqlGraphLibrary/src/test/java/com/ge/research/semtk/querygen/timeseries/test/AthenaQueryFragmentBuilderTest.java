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

package com.ge.research.semtk.querygen.timeseries.test;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;

import org.junit.Test;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.AthenaQueryFragmentBuilder;
import com.ge.research.semtk.utility.Utility;

public class AthenaQueryFragmentBuilderTest {

	@Test
	public void test() throws Exception{
		AthenaQueryFragmentBuilder fragmentBuilder = new AthenaQueryFragmentBuilder(); 
		LocalDateTime dateTime = LocalDateTime.parse("2016-04-07 14:16:17", Utility.DATETIME_FORMATTER_yyyyMMddHHmmss);
		assertEquals(fragmentBuilder.getFragmentForTimeCondition_typeTimestamp("timestampcol",">",dateTime), "(to_iso8601(\"timestampcol\") > '2016-04-07T14:16:17Z')");
	}
	
}
