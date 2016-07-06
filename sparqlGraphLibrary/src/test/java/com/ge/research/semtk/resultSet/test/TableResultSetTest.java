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


package com.ge.research.semtk.resultSet.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;


public class TableResultSetTest {

	
	@Test
	public void test() throws Exception{
		
		
		String jsonStr = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
		JSONObject jsonObj = (JSONObject) new JSONParser().parse(jsonStr);
		Table table = Table.fromJson(jsonObj);
		TableResultSet tableResultSet = new TableResultSet(true);
		tableResultSet.addResults(table);
		
		JSONObject tableResultSetJSON = tableResultSet.toJson();
		
		// rehydrate a TableResultSet from the JSON
		TableResultSet resultSetRehydrated1 = new TableResultSet();
		resultSetRehydrated1.readJson(tableResultSetJSON);
		assertEquals(resultSetRehydrated1.getTable().getNumColumns(),3);
		
		// rehydrate TableResultSet in a different way
		TableResultSet resultSetRehydrated2 = new TableResultSet(tableResultSetJSON);
		assertEquals(resultSetRehydrated2.getTable().getNumColumns(),3);
	}
	
	
	@Test
	public void testMerge() {
		try{
			
			String jsonStr1 = "{\"col_names\":[\"colA\",\"colB\",\"colC\"],\"rows\":[[\"apple\",\"banana\",\"coconut\"],[\"adam\",\"barbara\",\"chester\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":2}";
			JSONObject jsonObj1 = (JSONObject) new JSONParser().parse(jsonStr1);
			Table table1 = Table.fromJson(jsonObj1);
			TableResultSet tableResultSet1 = new TableResultSet();
			tableResultSet1.addResults(table1);
			
			String jsonStr2 = "{\"col_names\":[\"colC\",\"colB\",\"colA\"],\"rows\":[[\"cheesewhiz\",\"bonbons\",\"apple pie\"],[\"cider\",\"bourbon\",\"apple juice\"],[\"Chisholm\",\"Bobberson\",\"Anderson\"]],\"col_type\":[\"String\",\"String\",\"String\"],\"col_count\":3,\"row_count\":3}";
			JSONObject jsonObj2 = (JSONObject) new JSONParser().parse(jsonStr2);
			Table table2 = Table.fromJson(jsonObj2);
			TableResultSet tableResultSet2 = new TableResultSet();
			tableResultSet2.addResults(table2);
			
			ArrayList<TableResultSet> tableResultSets = new ArrayList<TableResultSet>();
			tableResultSets.add(tableResultSet1);
			tableResultSets.add(tableResultSet2);
			TableResultSet mergedTableResultSet = TableResultSet.merge(tableResultSets);
			
			assertEquals(mergedTableResultSet.getTable().getRows().size(),5);
			
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}	
	

}
