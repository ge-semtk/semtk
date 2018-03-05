/**
 ** Copyright 2018 General Electric Company
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

package com.ge.research.semtk.load.dataset.test;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;

import com.ge.research.semtk.load.dataset.TableDataset;
import com.ge.research.semtk.resultSet.Table;

public class TableDatasetTest {

	@Test
	public void test() throws Exception{
		
		String[] cols = {"colA","colB","colC"};
		String[] colTypes = {"String","String","String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rowFruit.add("banana");
		rowFruit.add("coconut");
		rows.add(rowFruit);
		ArrayList<String> rowNames = new ArrayList<String>();
		rowNames.add("adam");
		rowNames.add("barbara");
		rowNames.add("chester");
		rows.add(rowNames);
		ArrayList<String> rowLastNames = new ArrayList<String>();
		rowLastNames.add("adamson");
		rowLastNames.add("barberson");
		rowLastNames.add("chesterton");
		rows.add(rowLastNames);
		ArrayList<String> rowVegetables = new ArrayList<String>();
		rowVegetables.add("asparagus");
		rowVegetables.add("broccoli");
		rowVegetables.add("cauliflower");
		rows.add(rowVegetables);
		Table table = new Table(cols, colTypes, rows);
		
		TableDataset dataset = new TableDataset(table);		
		ArrayList<ArrayList<String>> datasetRows;
		
		ArrayList<String> datasetHeaders = dataset.getColumnNamesinOrder();
		assertEquals(datasetHeaders.get(0),"colA");
		assertEquals(datasetHeaders.get(1),"colB");
		assertEquals(datasetHeaders.get(2),"colC");
		
		datasetRows = dataset.getNextRecords(2);
		assertEquals(datasetRows.get(0),rowFruit);
		assertEquals(datasetRows.get(1),rowNames);
		datasetRows = dataset.getNextRecords(2);
		assertEquals(datasetRows.get(0),rowLastNames);
		assertEquals(datasetRows.get(1),rowVegetables);
		
		dataset.reset();
		datasetRows = dataset.getNextRecords(3);
		assertEquals(datasetRows.get(0),rowFruit);
		assertEquals(datasetRows.get(1),rowNames);
		assertEquals(datasetRows.get(2),rowLastNames);
		
		datasetRows = dataset.getNextRecords(3); 
		assertEquals(datasetRows.size(),1);		  // there is only one record left, even though we asked for 3
		assertEquals(datasetRows.get(0),rowVegetables);
		
		datasetRows = dataset.getNextRecords(3); 
		assertEquals(datasetRows.size(),0);		  // asked for data when there is none left

	}
	
	@Test 
	public void testOneRow() throws Exception{
		String[] cols = {"colA","colB","colC"};
		String[] colTypes = {"String","String","String"};
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		ArrayList<String> rowFruit = new ArrayList<String>();
		rowFruit.add("apple");
		rowFruit.add("banana");
		rowFruit.add("coconut");
		rows.add(rowFruit);
		Table table = new Table(cols, colTypes, rows);
		
		TableDataset dataset = new TableDataset(table);		
		ArrayList<ArrayList<String>> datasetRows;
		datasetRows = dataset.getNextRecords(2);
		assertEquals(datasetRows.size(), 1);
		assertEquals(datasetRows.get(0),rowFruit);
	}
}
