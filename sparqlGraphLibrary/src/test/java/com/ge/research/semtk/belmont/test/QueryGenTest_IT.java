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
import static org.junit.Assume.assumeFalse;

import static org.junit.Assert.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NoValidSparqlException;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

/**
 * Testing of querying features.
 * 
 * Ingestion and queries are performed without REST calls (DataLoader and SparqlEndpointInterface).
 * "Integration" is the triplestore.
 *  * 
 * @author 200001934
 *
 */
public class QueryGenTest_IT {
		
	private static SparqlGraphJson sgJsonChain = null;
	private static SparqlGraphJson sgJsonBattery = null;

	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		// load Chain
		sgJsonChain = TestGraph.initGraphWithData(QueryGenTest_IT.class, "chain");
		
		// load AnimalSubProps model 
		// and data:  Cats and Tigers
		TestGraph.uploadOwlContents(Utility.getResourceAsString(QueryGenTest_IT.class, 
				"AnimalSubProps.owl"));
		String csv = Utility.getResourceAsString(QueryGenTest_IT.class, 
				"animalSubPropsCats.csv");
		TestGraph.ingestCsvString(QueryGenTest_IT.class, 
				"animalSubPropsCats.json", csv);
		csv = Utility.getResourceAsString(QueryGenTest_IT.class, 
				"animalSubPropsTigers.csv");
		TestGraph.ingestCsvString(QueryGenTest_IT.class, 
				"animalSubPropsTigers.json", csv);
		
		TestGraph.uploadOwlContents(Utility.getResourceAsString(QueryGenTest_IT.class, 
				"Plant.owl"));
		csv = Utility.getResourceAsString(QueryGenTest_IT.class, 
				"plant_tulips.csv");
		TestGraph.ingestCsvString(QueryGenTest_IT.class, 
				"plant_tulips.json", csv);
		
		// load Batttery
		sgJsonBattery = TestGraph.addModelAndData(QueryGenTest_IT.class, "sampleBattery");

	}
	
	// TODO : all tests should have corresponding CONSTRUCT
	
	/**
	 * Basic query of tricky load
	 * @throws Exception
	 */
	
	@Test
	public void basicSelectQuery() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(4, tab.getNumColumns());
	}
	
	@Test
	public void selectWithType() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain.json");
		// Add a type to a node that doesn't need it's class determined
		ng.getNodeBySparqlID("Link_1").setIsTypeReturned(true); 
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		// should get same number of rows as basicSelectQuery and an extra column
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(5, tab.getNumColumns());
	}
	
	@Test
	public void selectSuperSubClass() throws Exception {
		Table tab;
		tab = TestGraph.execSelectFromResource(this.getClass(), "animalQuery.json");
		assertEquals("Unexpected number of Animals", 12, tab.getNumRows());
		
		tab = TestGraph.execSelectFromResource(this.getClass(), "animalQueryCat.json");
		assertEquals("Unexpected number of Cats", 10, tab.getNumRows());
		
		tab = TestGraph.execSelectFromResource(this.getClass(), "animalQueryTiger.json");
		assertEquals("Unexpected number of Tigers", 4, tab.getNumRows());
	}
	
	@Test
	public void returnTypeSelectQuery() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain.json");
		
		// set isTypeReturned and add an ORDER BY
		Node n = ng.getNodeBySparqlID("?Chain");
		n.setIsTypeReturned(true);
		ng.appendOrderBy(n.getTypeSparqlID());
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(5, tab.getNumColumns());
	}
	
	@Test
	public void optionalNode() throws Exception {
		
		// get every link regardless of whether it has a nextLink
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_optional.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(9, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void optionalProp() throws Exception {
		
		// get every chain regardless of whether it has a description
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_optional_prop.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void reverseOptionalNode() throws Exception {
		
		// get every pair of links regardless of whether they are directly attached to chain
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_rev_optional.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(6, tab.getNumRows());
		assertEquals(3, tab.getNumColumns());
	}
	
	@Test
	public void minusNode() throws Exception {
		
		// get every link regardless of whether it has a nextLink
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_minus.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(3, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void reverseMinusNode() throws Exception {
		
		// get every pair of links that is NOT attached to a chain
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_rev_minus.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(4, tab.getNumRows());
		assertEquals(3, tab.getNumColumns());
	}
	
	@Test
	public void selectFunctionGroupOrder() throws Exception {
		// count the number of links per chain and sort
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this, "/chain_count_links.json");

		TestGraph.queryAndCheckResults(sgjson, this, "/chain_count_links_results.csv");		// get every pair of links that is NOT attached to a chain
		
	}
	
	@Test
	public void qualifiersAndOptionalMinus() throws Exception {
		SparqlEndpointInterface sei =  TestGraph.getSei();
		
		// get every pair of links that is NOT attached to a chain
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_link.json");
		
		NodeItem nItem = ng.getNodeBySparqlID("?Link").getNodeItemList().get(0);
		Node targetNode = ng.getNodeBySparqlID("?Link_0");
		
		// set up many combinations of 
		//    optional/minus ; qualifier ; expected rows ; expected unique linkName; expected unique linkName_0; message
		String optminusQualifierRows[] = new String[]{
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";;6;6;6;Query with no optional/minus nor qualifier ", 
				
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";*;19;9;9;Query with no optional/minus and * qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";+;10;6;6;Query with no optional/minus and + qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";?;15;9;9;Query with no optional/minus and ? qualifier ", 
				String.valueOf(NodeItem.OPTIONAL_FALSE) + ";^;6;6;6;Query with no optional/minus and ^ qualifier ", 
				
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
			
			// TODO: what do * + ^ mean in a CONSTRUCT
			// TODO: how should sparql-generation respond?
			//
			//String sparql = ng.generateSparqlConstruct();
			//JSONArray graph =  sei.executeQueryToGraph(sparql);
			//assertTrue(true);
		}
	}
	
	@Test
	public void propertyOptionalMinus() throws Exception {
		SparqlEndpointInterface sei =  TestGraph.getSei();
		
		// get every pair of links that is NOT attached to a chain
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_chain.json");
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
		select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		tab = tRes.getTable();
		assertEquals("Property with no optional/minus returned the wrong number of rows", 2, tab.getNumRows());
		assertEquals("Property with no optional/minus returned the wrong number of columns", 2, tab.getNumColumns());
		
		// MINUS
		pItem.setOptMinus(PropertyItem.OPT_MINUS_MINUS);
		select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		tab = tRes.getTable();
		assertEquals("Property with no optional/minus returned the wrong number of rows", 1, tab.getNumRows());
		assertEquals("Property with no optional/minus returned the wrong number of columns", 2, tab.getNumColumns());
		assertTrue("Minus query return cell is not empty", tab.getCell(0, 1).isEmpty());
	}
	
	@Test
	public void constructPropertyOptionalMinus() throws Exception {
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
		
		// get every pair of links that is NOT attached to a chain
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_chain.json");
		PropertyItem pItem = ng.getNode(0).getPropertyItemBySparqlID("?chainDesc");
		
		pItem.setOptMinus(PropertyItem.OPT_MINUS_NONE);
		String sparql = ng.generateSparqlConstruct();
		JSONArray graph = TestGraph.getSei().executeQueryToGraph(sparql);		
		
		assertEquals("JSON-LD has wrong number of elements", 1, graph.size());
		assertEquals("Not all items have @types", 1, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 1, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of chainDesc attributes", 1, countAttributes(graph, "chainDesc", "descA"));
		
		// OPTIONAL
		pItem.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
		sparql = ng.generateSparqlConstruct();
		graph = TestGraph.getSei().executeQueryToGraph(sparql);	
		
		assertEquals("JSON-LD has wrong number of elements", 2, graph.size());
		assertEquals("Not all items have @types", 2, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 2, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of chainDesc->descA attributes", 1, countAttributes(graph, "chainDesc", "descA"));
		
		// MINUS
		pItem.setOptMinus(PropertyItem.OPT_MINUS_MINUS);
		sparql = ng.generateSparqlConstruct();
		graph = TestGraph.getSei().executeQueryToGraph(sparql);	
		
		assertEquals("JSON-LD has wrong number of elements", 1, graph.size());
		assertEquals("Not all items have @types", 1, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 1, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of chainDesc attributes", 0, countAttributes(graph, "chainDesc", null));
	}
	
	@Test
	public void unionTwoNodeItems() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/animalSubPropsCatNItemUnion.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
	}
	
	@Test
	public void constructUnionTwoNodeItems() throws Exception {
		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/animalSubPropsCatNItemUnion.json");
		String query = ng.generateSparqlConstruct();
		JSONArray graph = TestGraph.getSei().executeQueryToGraph(query);	

		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 4, graph.size());
		assertEquals("Not all items have @types", 4, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 4, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of name->beelz attributes", 1, countAttributes(graph, "name", "beelz"));
		assertEquals("Wrong number of name->white attributes", 1, countAttributes(graph, "name", "white"));
	}

	@Test
	/**
	 * Union of two nodes, where one isn't the "head node" because it has an incoming reverse minus
	 * @throws Exception
	 */
	public void unionTwoNodeItemsNonHead() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_node_minus_union.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(9, tab.getNumRows());
	}
	

	@Test
	public void constructUnionTwoNodeItemsNonHead() throws Exception {
		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chain_node_minus_union.json");
		String query = ng.generateSparqlConstruct();
		JSONArray graph = TestGraph.getSei().executeQueryToGraph(query);	

		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 9, graph.size());
		assertEquals("Not all items have @types", 9, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 9, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of linkName attributes", 9, countAttributes(graph, "linkName", null));
	}
	
	@Test
	public void unionTwoPropItems() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chainUnionProperties.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(2, tab.getNumRows());
	}
	
	@Test
	public void constructUnionTwoPropItems() throws Exception {
		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		// Union between a link and a chain
		// Each has their version of name bound to ?name
		// Link names need to contain "orphan" and chain names need to contain "B"  (sharing bound variables)

		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chainUnionProperties.json");
		String query = ng.generateSparqlConstruct();
		JSONArray graph = TestGraph.getSei().executeQueryToGraph(query);	

		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 2, graph.size());
		assertEquals("Not all items have @types", 2, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 2, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of chainName attributes", 1, countAttributes(graph, "chainName", null));
		assertEquals("Wrong number of chainDesc attributes", 1, countAttributes(graph, "chainDesc", null));

	}
	
	
	@Test
	public void unionTwoNodeSubgraphs() throws Exception {
		// Union between a link and a chain
		// Each has their version of name bound to ?name
		// Link names need to contain "orphan" and chain names need to contain "B"  (sharing bound variables)
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chainUnionNodes.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		Table tab = tRes.getTable();
		assertEquals(3, tab.getNumRows());
	}
	
	@Test
	public void constructUnionTwoNodeSubgraphs() throws Exception {
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
				
		// Union between a link and a chain
		// Each has their version of name bound to ?name
		// Link names need to contain "orphan" and chain names need to contain "B"  (sharing bound variables)
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/chainUnionNodes.json");
		String query = ng.generateSparqlConstruct();
		JSONArray graph = TestGraph.getSei().executeQueryToGraph(query);	
		
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 3, graph.size());
		assertEquals("Not all items have @types", 3, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 3, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of linkName attributes", 2, countAttributes(graph, "linkName", null));
		assertEquals("Wrong number of chainName attributes", 1, countAttributes(graph, "chainName", null));

	}
	
	
	@Test
	public void constructQuery() throws Exception{
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
		
		NodeGroup ng = sgJsonBattery.getNodeGroup();
		SparqlEndpointInterface sei = sgJsonBattery.getSparqlConn().getDefaultQueryInterface();

		String query = ng.generateSparqlConstruct();
		JSONObject responseJson = sei.executeQuery(query, SparqlResultTypes.GRAPH_JSONLD);
		
		JSONArray graph = (JSONArray)responseJson.get("@graph");
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 9, graph.size());
		assertEquals("Not all items have @types", 9, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 9, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of cellId attributes", 4, countAttributes(graph, "cellId", null));
		assertEquals("Wrong number of color attributes", 4, countAttributes(graph, "color", null));
		assertEquals("Wrong number of name attributes", 2, countAttributes(graph, "name", null));
	}

	
	@Test
	public void constructQueryWithConstraints() throws Exception {		
	
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/sampleBattery_PlusConstraints.json");		
		SparqlEndpointInterface sei = sgJsonBattery.getSparqlConn().getDefaultQueryInterface();
		
		String query = ng.generateSparqlConstruct();
		
		JSONObject responseJson = sei.executeQuery(query, SparqlResultTypes.GRAPH_JSONLD);
		JSONArray graph = (JSONArray)responseJson.get("@graph");
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 3, graph.size());
		assertEquals("Not all items have @types", 3, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 3, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of name attributes", 1, countAttributes(graph, "name", null));
		assertEquals("Wrong number of cellId attributes", 1, countAttributes(graph, "cellId", null));
		assertEquals("Wrong number of color attributes", 1, countAttributes(graph, "color", null));
	}
	
	@Test
	public void constructWhere() throws Exception {		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
				
		// Prevent recurrence of bug in CONSTRUCT when nodes have bindings

		JSONArray res = TestGraph.execConstructFromResource(this.getClass(), "chain_construct_where1.json");
		String resStr = res.toJSONString();
		
		assertEquals("Construct returned wrong number of items", 4, res.size());
		assertTrue("Construct results do not contain linkB1", resStr.contains("linkB1"));
		assertTrue("Construct results do not contain linkB2", resStr.contains("linkB2"));
		assertTrue("Construct results do not contain linkB3", resStr.contains("linkB3"));
		assertTrue("Construct results do not contain linkB4", resStr.contains("linkB4"));
		
	}
	
	@Test
	public void superPropertyQueries() throws Exception {		
		// granny mom, greymom, white are names of cats with kitties (a type of child)
		// fluffy mom is a cat with a kitty nutter
		//                          a demon (type of child) beelz
		// tigger is a tiger (type of cat)
		//       with demon (type of child) Animal tiger cub
		// tigger also has a "scary name" (type of name) "scary tigger"
		
		// try it as select
		Table res = TestGraph.execSelectFromResource(this.getClass(), "animalSubPropsCatHasChildAnimal.json");
		assertEquals("Table has wrong number of rows", 6, res.getNumRows());
	}
	
	@Test
	public void constructSuperPropertyQueries() throws Exception {		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
		
		// try it as construct
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this.getClass(), "animalSubPropsCatHasChildAnimal.json");
		NodeGroup ng = sgJson.getNodeGroup();
		testCatHasChildrenConstruct(ng);
	}
	
	@Test
	public void constructWithBinding() throws Exception {		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
				
		// Prevent recurrence of bug in CONSTRUCT when nodes have bindings

		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromResource(this.getClass(), "animalSubPropsCatHasChildAnimal.json");
		NodeGroup ng = sgJson.getNodeGroup();
		ng.setBinding(ng.getNodeBySparqlID("Animal"), "ChildAnimal");
		
		testCatHasChildrenConstruct(ng);
		
	}
	
	/**
	 * Test the nodegroup construct returns with and without oInfo
	 * @param ng
	 * @throws Exception
	 */
	private static void testCatHasChildrenConstruct(NodeGroup ng) throws Exception {
		// make sure oInfo is associated
		ng.noInflateNorValidate(TestGraph.getOInfo());
		String sparql = ng.generateSparqlConstruct();
		
		// run construct:  subject to virtuoso incomplete results problem.  assume away if needed
		JSONArray graph = TestGraph.getSei().executeQueryToGraph(sparql);
		assertEquals("JSON-LD has wrong number of elements", 8, graph.size());
		assertEquals("Not all items have @types", 8, countAttributes(graph, "@type", null));
		assertEquals("Not all items have @id", 8, countAttributes(graph, "@id", null));
		assertEquals("Missing name attributes", 8, countAttributes(graph, "name", null));
		assertEquals("Missing hasKitties attributes", 3, countAttributes(graph, "hasKitties", null));
		assertEquals("Missing hasDemons attributes", 2, countAttributes(graph, "hasDemons", null));
		assertEquals("Missing scaryName attributes", 1, countAttributes(graph, "scaryName", null));

		
		// try without oInfo
		ng.getSparqlConnection().setOwlImportsEnabled(false);
		ng.noInflateNorValidate(null);
		sparql = ng.generateSparqlConstruct();
		
		// run construct:  subject to virtuoso incomplete results problem.  assume away if needed
		graph = TestGraph.getSei().executeQueryToGraph(sparql);
		
		// no oInfo will not use sub properties, so results are empty
		// if this is fixed, then you'd get the same results as above.
		assertEquals("JSON-LD has wrong number of elements", 0, graph.size());
	}
	
	@Test
	public void subPropertyQueries() throws Exception {		
		// Using superPropertyQueries data
		//
		// Only get "hasDemon" not parent property "hasChild"
		// Since tigger also has "scary name" he will appear once for each name
		Table res = TestGraph.execSelectFromResource(this.getClass(), "animalSubPropsCatHasDemon.json");
		assertEquals("Table has wrong number of rows", 3, res.getNumRows());
		
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		JSONArray graph = TestGraph.execConstructFromResource(this.getClass(), "animalSubPropsCatHasDemon.json");
		assertEquals("JSON-LD has wrong number of elements", 4, graph.size());
	}
	
	@Test
	public void subDataPropertyQueries() throws Exception {		
		// Two tiggers have names (super prop) 
		// and scary names (sub prop)
		// get just the scary names
		Table res = TestGraph.execSelectFromResource(this.getClass(), "animalSubPropsScaryNames.json");
		assertEquals("Table has wrong number of rows", 2, res.getNumRows());
		
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		JSONArray graph = TestGraph.execConstructFromResource(this.getClass(), "animalSubPropsScaryNames.json");
		assertEquals("JSON-LD has wrong number of elements", 2, graph.size());
		
		assertFalse("Construct query returned a super prop name Richard", graph.toJSONString().contains("Richard"));
	}
	
	@Test
	public void testGroupBy() throws Exception {	
		// nodegroup has:  two functions, GROUP BY, and ORDER BY
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this.getClass(), "plant_tulips.json");
		Table res = TestGraph.execTableSelect(sgjson);
		TestGraph.queryAndCheckResults(sgjson, this, "plant_tulips_group1_results.csv");
	}
	
	@Test
	public void testCheckNodegroup() throws Exception {	
		
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this.getClass(), "plant_tulips.json");
		NodeGroup ng = sgjson.getNodeGroup();
		OntologyInfo oInfo = TestGraph.getOInfo();
		ng.noInflateNorValidate(oInfo);
		// no message
		try {
			ng.generateSparqlSelect();
		} catch (NoValidSparqlException e) {
			assertTrue("Found unexpected ng.checkNOdegroup() message: " + e.getMessage(), false);
		}
		
		
		// nothing to return
		ng.unsetAllReturns();
		try {
			ng.generateSparqlSelect();
			assertTrue("No exception was thrown generating sparql with nothing to return", false);
		} catch (NoValidSparqlException e) {
			assertTrue("Expected 'nothing to return' ng.checkNOdegroup() message, got: " + e.getMessage(), e.getMessage().contains("return"));
		}
		
		
		// nothing to delete
		try {
			String msg = ng.generateSparqlDelete();
			assertTrue("No exception was thrown generating sparql with nothing to delete", false);
		} catch (NoValidSparqlException e) {
			assertTrue("Expected 'nothing to DELETE' ng.checkNodegroup() message, got: " + e.getMessage(), e.getMessage().contains("delet"));
		}

		// reload and do some more
		sgjson = TestGraph.getSparqlGraphJsonFromResource(this.getClass(), "plant_tulips.json");
		ng = sgjson.getNodeGroup();
		ng.noInflateNorValidate(oInfo);
		
		// functions without GROUP BY
		ng.clearGroupBy();
		try {
			String msg = ng.generateSparqlSelect();
			assertTrue("No exception was thrown generating sparql with missing GROUP BY", false);
		} catch (NoValidSparqlException e) {
			assertTrue("Expected 'GROUP BY' ng.checkNodegroup() message, got: " + e.getMessage(), e.getMessage().contains("GROUP BY"));
		}
		
	}
	
	@Test
	public void test_createConstructAllConnectedCell() throws Exception {	
		// test where cell has incoming and outgoing connections
		
		final String CELL = "http://kdl.ge.com/batterydemo#Cell";
		OntologyInfo oInfo = TestGraph.getOInfo();
		NodeGroup ng1 = new NodeGroup();
		ng1.setSparqlConnection(TestGraph.getSparqlConn());
		Node node = ng1.addNode(CELL, oInfo);
		ng1.setIsReturned(node, true);
		ng1.orderByAll();
		
		Table tab = TestGraph.execTableSelect(ng1.generateSparqlSelect());
		String instanceUri = tab.getCell(0, 0);
		
		NodeGroup ng = NodeGroup.createConstructAllConnected(
							CELL, instanceUri, 
							TestGraph.getSparqlConn(), TestGraph.getOInfo(), TestGraph.getPredicateStats()
							);
		
		JSONArray jArr = TestGraph.execJsonConstruct(ng);
		String res = jArr.toJSONString();
		for (String lookup : new String [] {"battA", "red", "cellId", "color", "\"cell200\""}) {
			assertTrue("Results are missing: " + lookup, res.contains(lookup));
		}
	}
	
	@Test
	public void test_createConstructAllConnectedBattery() throws Exception {	
		final String BATTERY = "http://kdl.ge.com/batterydemo#Battery";
		// test where all Battery links are out-going
		OntologyInfo oInfo = TestGraph.getOInfo();
		NodeGroup ng1 = new NodeGroup();
		ng1.setSparqlConnection(TestGraph.getSparqlConn());
		Node battNode = ng1.addNode(BATTERY, oInfo);
		ng1.setIsReturned(battNode, true);
		ng1.orderByAll();
		
		Table tab = TestGraph.execTableSelect(ng1.generateSparqlSelect());
		String instanceUri = tab.getCell(0, 0);
		
		NodeGroup ng = NodeGroup.createConstructAllConnected(
				BATTERY, instanceUri, 
							TestGraph.getSparqlConn(), TestGraph.getOInfo(), TestGraph.getPredicateStats()
							);
		
		JSONArray jArr = TestGraph.execJsonConstruct(ng);
		String res = jArr.toJSONString();
		for (String lookup : new String [] {"Cell_cell300", "battA", "1966", "Cell_cell200", "1979"}) {
			assertTrue("Results are missing: " + lookup, res.contains(lookup));
		}
	}
	/**
	 * Count the number of attributes of items in a Graph where key.contains(keyContains) and value.contains(valContains)
	 * Not recursive, checks each element in the graph array.
	 * @param graph
	 * @param keyContains
	 * @param valContains
	 * @return
	 */
	private static int countAttributes(JSONArray graph, String keyContains, String valContains) {
		int ret = 0;
		// loop through the graph array of objects
		for (Object o : graph) {
			JSONObject j = (JSONObject) o;
			for (Object key : j.keySet()) {
				if (keyContains == null || ((String) key).contains(keyContains)   &&  (valContains == null || ((String) j.get(key)).contains(valContains))) {
					ret += 1;
				}
			}
		}
		return ret;
	}
}
