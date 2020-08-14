/**
 ** Copyright 2016-2020 General Electric Company
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

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

/**
 * As of Dec 2018, most advanced integration testing of ingestion and querying features.
 * 
 * Ingestion and queries are performed without REST calls (DataLoader and SparqlEndpointInterface).
 * "Integration" is the triplestore.
 * 
 * Test query functions by running them and checking the number of returns.  NOT by inspecting SPARQL.
 * 
 * @author 200001934
 *
 */
public class LoadAndQueryGenTest_IT {
		
	private static SparqlGraphJson sgJson = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		// This nodegroup includes multiple nodes with the same type
		// and URI lookup where different nodes with the same type
		// can represent the same URI on different lines of input.
		
		sgJson = TestGraph.initGraphWithData(LoadAndQueryGenTest_IT.class, "chain");
		
		// load another model
		TestGraph.uploadOwlContents(Utility.getResourceAsString(LoadAndQueryGenTest_IT.class, 
				"AnimalSubProps.owl"));
		String csv = Utility.getResourceAsString(LoadAndQueryGenTest_IT.class, 
				"animalSubPropsCats.csv");
		TestGraph.ingestCsvString(LoadAndQueryGenTest_IT.class, 
				"animalSubPropsCats.json", csv);
	}
	
	
	/**
	 * Basic query of tricky load
	 * @throws Exception
	 */
	
	@Test
	public void basicSelectQuery() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(4, tab.getNumColumns());
	}
	
	@Test
	public void returnTypeSelectQuery() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain.json");
		
		// set isTypeReturned and add an ORDER BY
		Node n = ng.getNodeBySparqlID("?Chain");
		n.setIsTypeReturned(true);
		ng.appendOrderBy(n.getTypeSparqlID());
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(5, tab.getNumColumns());
	}
	
	@Test
	public void optionalNode() throws Exception {
		
		// get every link regardless of whether it has a nextLink
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain_optional.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(9, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void optionalProp() throws Exception {
		
		// get every chain regardless of whether it has a description
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain_optional_prop.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void reverseOptionalNode() throws Exception {
		
		// get every pair of links regardless of whether they are directly attached to chain
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain_rev_optional.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(6, tab.getNumRows());
		assertEquals(3, tab.getNumColumns());
	}
	
	@Test
	public void minusNode() throws Exception {
		
		// get every link regardless of whether it has a nextLink
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain_minus.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(3, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void reverseMinusNode() throws Exception {
		
		// get every pair of links that is NOT attached to a chain
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain_rev_minus.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(4, tab.getNumRows());
		assertEquals(3, tab.getNumColumns());
	}
	
	@Test
	public void qualifiersAndOptionalMinus() throws Exception {
		SparqlEndpointInterface sei =  TestGraph.getSei();
		
		// get every pair of links that is NOT attached to a chain
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain_link.json");
		
		NodeItem nItem = ng.getNodeBySparqlID("?Link").getNodeItemList().get(0);
		Node targetNode = ng.getNodeBySparqlID("?Link_0");
		
		// set up many combinations of 
		//    optional/minus ; qualifier ; expected rows ; expected unique linkName; expected unique linkName_0; message
		String optminusQualifierRows[] = new String[]{
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";;6;6;6;Query with no optional/minus nor qualifier ", 
				
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";*;19;9;9;Query with no optional/minus * qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";+;10;6;6;Query with no optional/minus + qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";?;15;9;9;Query with no optional/minus ? qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";^;6;6;6;Query with no optional/minus ^ qualifier ", 
				
				String.valueOf(NodeItem.OPTIONAL_TRUE) + ";;9;9;7;Query with optional and no qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_TRUE) + ";*;19;9;9;Query with optional and * qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_TRUE) + ";+;13;9;7;Query with optional and + qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_TRUE) + ";?;15;9;9;Query with optional and ? qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_TRUE) + ";^;9;9;7;Query with optional and ^ qualifier ", 
				
				String.valueOf(NodeItem.OPTIONAL_REVERSE) + ";;9;7;9;Query with optional reverse and no qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_REVERSE) + ";*;19;9;9;Query with optional reverse and * qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_REVERSE) + ";+;13;7;9;Query with optional reverse and + qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_REVERSE) + ";?;15;9;9;Query with optional reverse and ? qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_REVERSE) + ";^;9;7;9;Query with optional reverse and ^ qualifier ", 
				
				String.valueOf(NodeItem.MINUS_TRUE) + ";;3;3;1;Query with minus and no qualifier ", 
				String.valueOf(NodeItem.MINUS_TRUE) + ";*;0;0;0;Query with minus and * qualifier ", 
				String.valueOf(NodeItem.MINUS_TRUE) + ";+;3;3;1;Query with minus and + qualifier ", 
				String.valueOf(NodeItem.MINUS_TRUE) + ";?;0;0;0;Query with minus and ? qualifier ", 
				String.valueOf(NodeItem.MINUS_TRUE) + ";^;3;3;1;Query with minus and ^ qualifier ", 
				
				String.valueOf(NodeItem.MINUS_REVERSE) + ";;3;1;3;Query with minus reverse and no qualifier ", 
				String.valueOf(NodeItem.MINUS_REVERSE) + ";*;0;0;0;Query with minus reverse and * qualifier ", 
				String.valueOf(NodeItem.MINUS_REVERSE) + ";+;3;1;3;Query with minus reverse and + qualifier ", 
				String.valueOf(NodeItem.MINUS_REVERSE) + ";?;0;0;0;Query with minus reverse and ? qualifier ", 
				String.valueOf(NodeItem.MINUS_REVERSE) + ";^;3;1;3;Query with minus reverse and ^ qualifier ", 

		};
		
		// run each combination
		for (String testCase : optminusQualifierRows) {
			String f[] = testCase.split(";");
			int optMinus = Integer.parseInt(f[0]);
			String qual = f[1];
			int expectedRows = Integer.parseInt(f[2]);
			int unique1 = Integer.parseInt(f[3]);
			int unique2 = Integer.parseInt(f[4]);
			String msg = f[5];

			nItem.setOptionalMinus(targetNode, optMinus);
			nItem.setQualifier(targetNode, qual);
			
			String select = ng.generateSparqlSelect();
			TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
			
			Table tab = tRes.getTable();
			
			assertEquals(msg + "returned unexpected number of rows", expectedRows, tab.getNumRows());
			int actualUnique1 = tab.getColumnUniqueValues("linkName").length;
			int actualUnique2 = tab.getColumnUniqueValues("linkName_0").length;
			assertEquals(msg + "returned unexpected number of unique values in linkName", unique1, actualUnique1);
			assertEquals(msg + "returned unexpected number of unique values in linkName_0", unique2, actualUnique2);
		}
	}
	
	@Test
	public void propertyOptionalMinus() throws Exception {
		SparqlEndpointInterface sei =  TestGraph.getSei();
		
		// get every pair of links that is NOT attached to a chain
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/chain_chain.json");
		PropertyItem pItem = ng.getNode(0).getPropertyItemBySparqlID("?chainDesc");
		
		pItem.setOptMinus(PropertyItem.OPT_MINUS_NONE);
		String select = ng.generateSparqlSelect();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals("Property with no optional/minus returned the wrong number of rows", 1, tab.getNumRows());
		assertEquals("Property with no optional/minus returned the wrong number of columns", 2, tab.getNumColumns());
		assertTrue("Empty cell returned non-optional query.", !tab.getCell(0, 0).isEmpty() && !tab.getCell(0, 1).isEmpty());
		
		// OPTIONAL
		pItem.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
		select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		tab = tRes.getTable();
		assertEquals("Property with no optional/minus returned the wrong number of rows", 2, tab.getNumRows());
		assertEquals("Property with no optional/minus returned the wrong number of columns", 2, tab.getNumColumns());
		
		// MINUS
		pItem.setOptMinus(PropertyItem.OPT_MINUS_MINUS);
		select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		tab = tRes.getTable();
		assertEquals("Property with no optional/minus returned the wrong number of rows", 1, tab.getNumRows());
		assertEquals("Property with no optional/minus returned the wrong number of columns", 2, tab.getNumColumns());
		assertTrue("Minus query return cell is not empty", tab.getCell(0, 1).isEmpty());
	}
	
	@Test
	public void unionTwoNodeItems() throws Exception {
		
		// get every pair of links regardless of whether they are directly attached to chain
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/animalSubPropsCatNItemUnion.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(3, tab.getNumColumns());
	}
}
