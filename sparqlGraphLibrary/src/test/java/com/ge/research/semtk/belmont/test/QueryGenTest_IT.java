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

	public static final String NON_TREE_GRAPHS_DIR = "non-tree-graphs";
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		
		/*
		 * In this Unit, all the owl and data is pushed up-front.
		 * Tests don't load additional owl nor data, nor do they clear the graph.
		 */
		
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

		// non-tree graph 
		TestGraph.uploadOwlContents(
				Utility.getResourceAsString(
						QueryGenTest_IT.class, 
						String.format("%s/non-tree-graphs.owl", NON_TREE_GRAPHS_DIR)
				)
		);

		// load RangeTest (owl contains model and a little data)
		TestGraph.uploadOwlContents(Utility.getResourceAsString(QueryGenTest_IT.class, "RangeTest.owl"));

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
		assertTrue("row 0 col 0 is blank", !tab.getCell(0, 0).isBlank());
		assertTrue("row 0 col 1 is blank", !tab.getCell(0, 1).isBlank());
		assertEquals(5, tab.getNumColumns());
	}
	
	/**
	 * Past bugs have shown that using an oInfo generating SPARQL on a leaf node
	 * is an extra risk for bugs in returning type.
	 * @throws Exception
	 */
	@Test
	public void selectTigerWithType() throws Exception {
		
		NodeGroup ng = new NodeGroup();
		ng.setSparqlConnection(TestGraph.getSparqlConn());
		ng.noInflateNorValidate(TestGraph.getOInfo());
		Node tiger = ng.addNode("http://AnimalSubProps#Tiger", TestGraph.getOInfo());
		tiger.setIsReturned(true);
		tiger.setIsTypeReturned(true);
		
		Table tab = TestGraph.execQueryToTable(ng.generateSparqlSelect());
		
		assertEquals(2, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
		assertTrue("row 0 uri is blank", !tab.getCell(0, "Tiger").isBlank());
		assertTrue("row 0 type is blank", !tab.getCell(0, "Tiger_type").isBlank());
		
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
		Table tab = TestGraph.execSelectFromResource(this, "/chain_optional.json");
		
		assertEquals(9, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void optionalProp() throws Exception {
		
		// get every chain regardless of whether it has a description
		Table tab = TestGraph.execSelectFromResource(this, "/chain_optional_prop.json");
		
		assertEquals(2, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void reverseOptionalNode() throws Exception {
		
		// get every pair of links regardless of whether they are directly attached to chain
		Table tab = TestGraph.execSelectFromResource(this, "/chain_rev_optional.json");
		
		assertEquals(6, tab.getNumRows());
		assertEquals(3, tab.getNumColumns());
	}
	
	@Test
	public void minusNode() throws Exception {
		
		// get every link regardless of whether it has a nextLink
		Table tab = TestGraph.execSelectFromResource(this, "/chain_minus.json");
		
		assertEquals(3, tab.getNumRows());
		assertEquals(2, tab.getNumColumns());
	}
	
	@Test
	public void reverseMinusNode() throws Exception {
		
		// get every pair of links that is NOT attached to a chain
		Table tab = TestGraph.execSelectFromResource(this, "/chain_rev_minus.json");
		
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
			
			if (expectedRows == 0) {
				assertEquals(msg + " expected no returns got " + tab.toCSVString(), 0, tab.getNumRows());
			} else {
				int actualUnique1 = tab.getColumnUniqueValues("linkName").length;
				int actualUnique2 = tab.getColumnUniqueValues("linkName_0").length;
				assertEquals(msg + "returned unexpected number of unique values in linkName", unique1, actualUnique1);
				assertEquals(msg + "returned unexpected number of unique values in linkName_0", unique2, actualUnique2);
			}
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
		Table tab = TestGraph.execConstructToNT(ng);
		
		assertEquals("JSON-LD has wrong number of elements", 1, countInstances(tab));
		assertEquals("Wrong number of chainDesc attributes", 1, countAttributes(tab, "chainDesc", "descA"));
		
		// OPTIONAL
		pItem.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
		tab = TestGraph.execConstructToNT(ng);
		
		assertEquals("JSON-LD has wrong number of elements", 2,  countInstances(tab));
		assertEquals("Wrong number of chainDesc->descA attributes", 1, countAttributes(tab, "chainDesc", "descA"));
		
		// MINUS
		pItem.setOptMinus(PropertyItem.OPT_MINUS_MINUS);
		tab = TestGraph.execConstructToNT(ng);

		
		assertEquals("JSON-LD has wrong number of elements", 1, countInstances(tab));
		assertEquals("Wrong number of chainDesc attributes", 0, countAttributes(tab, "chainDesc", null));
	}
	
	@Test
	public void unionTwoNodeItems() throws Exception {
		
		Table tab = TestGraph.execSelectFromResource(this, "/animalSubPropsCatNItemUnion.json");
		
		assertEquals(2, tab.getNumRows());
	}
	
	@Test
	public void constructUnionTwoNodeItems() throws Exception {
		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		Table tab = TestGraph.execConstructToNTFromResource(this.getClass(), "/animalSubPropsCatNItemUnion.json");

		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 4, countInstances(tab));
		assertEquals("Wrong number of name->beelz attributes", 1, countAttributes(tab, "name", "beelz"));
		assertEquals("Wrong number of name->white attributes", 1, countAttributes(tab, "name", "white"));
	}

	@Test
	/**
	 * Union of two nodes, where one isn't the "head node" because it has an incoming reverse minus
	 * @throws Exception
	 */
	public void unionTwoNodeItemsNonHead() throws Exception {
		
		Table tab = TestGraph.execSelectFromResource(this, "/chain_node_minus_union.json");
		
		assertEquals(9, tab.getNumRows());
	}
	

	@Test
	public void constructUnionTwoNodeItemsNonHead() throws Exception {
		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		Table tab = TestGraph.execConstructToNTFromResource(this.getClass(), "/chain_node_minus_union.json");


		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 9, countInstances(tab));
		assertEquals("Wrong number of linkName attributes", 9, countAttributes(tab, "#linkName>", null));
	}
	
	@Test
	public void nestedUnion() throws Exception {
		
		// get every pair of links regardless of whether they are directly attached to chain
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/animalSubPropsNestedUnion.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJsonChain.getSparqlConn().getDefaultQueryInterface();
		TableResultSet tRes = (TableResultSet) sei.executeQueryAndBuildResultSet(select, SparqlResultTypes.TABLE);
		
		// not sure the meaning of this nested union query
		// but if it got here, at least it didn't throw an error
	}
	
	@Test
	public void unionTwoPropItems() throws Exception {
		
		Table tab = TestGraph.execSelectFromResource(this, "/chainUnionProperties.json");
		
		assertEquals(2, tab.getNumRows());
	}
	
	@Test
	public void constructUnionTwoPropItems() throws Exception {
		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		// Union between a link and a chain
		// Each has their version of name bound to ?name
		// Link names need to contain "orphan" and chain names need to contain "B"  (sharing bound variables)

		Table tab = TestGraph.execConstructToNTFromResource(this.getClass(), "/chainUnionProperties.json");

		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 2, countInstances(tab));
		assertEquals("Wrong number of chainName attributes", 1, countAttributes(tab, "#chainName>", null));
		assertEquals("Wrong number of chainDesc attributes", 1, countAttributes(tab, "#chainDesc>", null));

	}
	
	
	@Test
	public void unionTwoNodeSubgraphs() throws Exception {
		// Union between a link and a chain
		// Each has their version of name bound to ?name
		// Link names need to contain "orphan" and chain names need to contain "B"  (sharing bound variables)
		
		Table tab = TestGraph.execSelectFromResource(this, "/chainUnionNodes.json");
		
		assertEquals(3, tab.getNumRows());
	}
	
	@Test
	public void constructUnionTwoNodeSubgraphs() throws Exception {
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
				
		// Union between a link and a chain
		// Each has their version of name bound to ?name
		// Link names need to contain "orphan" and chain names need to contain "B"  (sharing bound variables)
		
		Table tab = TestGraph.execConstructToNTFromResource(this.getClass(), "/chainUnionNodes.json");
		
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 3, countInstances(tab));
		assertEquals("Wrong number of linkName attributes", 2, countAttributes(tab, "#linkName>", null));
		assertEquals("Wrong number of chainName attributes", 1, countAttributes(tab, "#chainName>", null));

	}
	
	
	@Test
	public void constructQuery() throws Exception{
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
		
		NodeGroup ng = sgJsonBattery.getNodeGroup();
		SparqlEndpointInterface sei = sgJsonBattery.getSparqlConn().getDefaultQueryInterface();

		String query = ng.generateSparqlConstruct();
		Table tab = sei.executeQueryToNTriplesTable(query);

		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 9, countInstances(tab));
		assertEquals("Wrong number of cellId attributes", 4, countAttributes(tab, "#cellId>", null));
		assertEquals("Wrong number of color attributes", 4, countAttributes(tab, "#color>", null));
		assertEquals("Wrong number of name attributes", 2, countAttributes(tab, "#name>", null));
	}

	
	@Test
	public void constructQueryWithConstraints() throws Exception {		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/sampleBattery_PlusConstraints.json");		
		SparqlEndpointInterface sei = sgJsonBattery.getSparqlConn().getDefaultQueryInterface();
		
		String query = ng.generateSparqlConstruct();
		query = IntegrationTestUtility.replaceGeneratedPrefix(query);
		
		Table tab = sei.executeQueryToNTriplesTable(query);
		
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 3, countInstances(tab));
		//assertEquals("Not all items have @types", 3, countAttributes(graph, "@type", null));
		//assertEquals("Not all items have @id", 3, countAttributes(graph, "@id", null));
		assertEquals("Wrong number of name attributes", 1, countAttributes(tab, "#name>", null));
		assertEquals("Wrong number of cellId attributes", 1, countAttributes(tab, "#cellId>", null));
		assertEquals("Wrong number of color attributes", 1, countAttributes(tab, "#color>", null));
	}
	
	@Test
	public void constructWhere() throws Exception {		
				
		// Prevent recurrence of bug in CONSTRUCT when nodes have bindings

		Table tab = TestGraph.execConstructToNTFromResource(this.getClass(), "chain_construct_where1.json");
		String tabStr = tab.toCSVString();
		
		assertEquals("Construct returned wrong number of items", 4, countInstances(tab));
		assertTrue("Construct results do not contain linkB1", tabStr.contains("linkB1"));
		assertTrue("Construct results do not contain linkB2", tabStr.contains("linkB2"));
		assertTrue("Construct results do not contain linkB3", tabStr.contains("linkB3"));
		assertTrue("Construct results do not contain linkB4", tabStr.contains("linkB4"));
		
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
		Table tab = TestGraph.execConstructToNT(ng);
		
		// run construct:  subject to virtuoso incomplete results problem.  assume away if needed
		assertEquals("JSON-LD has wrong number of elements", 8, countInstances(tab));
		assertEquals("Missing name attributes", 8, countAttributes(tab, "name", null));
		assertEquals("Missing hasKitties attributes", 3, countAttributes(tab, "hasKitties", null));
		assertEquals("Missing hasDemons attributes", 2, countAttributes(tab, "hasDemons", null));
		assertEquals("Missing scaryName attributes", 1, countAttributes(tab, "scaryName", null));

		
		// try without oInfo
		ng.getSparqlConnection().setOwlImportsEnabled(false);
		ng.noInflateNorValidate(null);
		tab = TestGraph.execConstructToNTUninflated(ng);
		
		// no oInfo will not use sub properties, so results are empty
		// if this is fixed, then you'd get the same results as above.
		assertEquals("JSON-LD has wrong number of elements", 0, countInstances(tab));
	}
	
	@Test
	public void subPropertyQueries() throws Exception {		
		// Using superPropertyQueries data
		//
		// Only get "hasDemon" not parent property "hasChild"
		// Since tigger also has "scary name" he will appear once for each name
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));

		Table res = TestGraph.execSelectFromResource(this.getClass(), "animalSubPropsCatHasDemon.json");

		assertEquals("Table has wrong number of rows", 3, res.getNumRows());

		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		Table tab = TestGraph.execConstructToNTFromResource(this.getClass(), "animalSubPropsCatHasDemon.json");
		assertEquals("JSON-LD has wrong number of elements", 4, countInstances(tab));
	}
	
	@Test
	public void subDataPropertyQueries() throws Exception {		
		// Two tiggers have names (super prop) 
		// and scary names (sub prop)
		// get just the scary names
		
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
						
		Table res = TestGraph.execSelectFromResource(this.getClass(), "animalSubPropsScaryNames.json");
		assertEquals("Table has wrong number of rows", 2, res.getNumRows());
		
		// try it as construct:  subject to virtuoso incomplete results problem.  assume away if needed
		Table tab = TestGraph.execConstructToNTFromResource(this.getClass(), "animalSubPropsScaryNames.json");
		assertEquals("JSON-LD has wrong number of elements", 2, countInstances(tab));
		
		assertFalse("Construct query returned a super prop name Richard", tab.toCSVString().contains("Richard"));
	}
	
	@Test
	public void testGroupBy() throws Exception {	
		// nodegroup has:  two functions, GROUP BY, and ORDER BY
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this.getClass(), "plant_tulips.json");
		Table res = TestGraph.execTableSelect(sgjson);
		
		// query contains MAX called on a float and AVG called on integer
		// but all the values work out as integers
		// Fuseki returns them all as floats, e.g. 2.0
		// MarkLogic returns them as integers, e.g. 2
		// Not sure if one is really wrong.
		
		assertEquals("wrong number of results rows", 4, res.getNumRows());
		Integer col0[] = new Integer [] { 2,3,4,5};
		Integer col1[] = new Integer [] { 8,9,10,11};
		for (int i=0; i< col0.length; i++ ) {
			// actual values start as Double and cast to int.
			assertEquals("wrong value in cell column 0 row " + String.valueOf(i), (int) col0[i], (int) Double.parseDouble(res.getCell(i, 0)));
			assertEquals("wrong value in cell column 1 row " + String.valueOf(i), (int) col1[i], (int) Double.parseDouble(res.getCell(i, 1)));

		}
		
		//TestGraph.queryAndCheckResults(sgjson, this, "plant_tulips_group1_results.csv");
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
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
				
		
		// test where cell has incoming and outgoing connections
		
		final String CELL = "http://kdl.ge.com/batterydemo#Cell";
		OntologyInfo oInfo = TestGraph.getOInfo();
		NodeGroup ng1 = new NodeGroup();
		ng1.setSparqlConnection(TestGraph.getSparqlConn());
		Node node = ng1.addNode(CELL, oInfo);
		ng1.setIsReturned(node, true);
		ng1.orderByAll();
		
		Table tab = TestGraph.execQueryToTable(ng1.generateSparqlSelect());
		String instanceUri = tab.getCell(0, 0);
		
		NodeGroup ng = NodeGroup.createConstructAllConnected(
							CELL, instanceUri, 
							TestGraph.getSparqlConn(), TestGraph.getOInfo(), TestGraph.getPredicateStats()
							);
		
		Table tab2 = TestGraph.execConstructToNT(ng);
		String res = tab2.toCSVString();
		for (String lookup : new String [] {"battA", "red", "cellId", "color", "\"cell200\""}) {
			assertTrue("Results are missing: " + lookup, res.contains(lookup));
		}
	}
	
	@Test
	public void test_createConstructAllConnectedBattery() throws Exception {	
		// virtuoso is unreliable with CONSTRUCT
		assumeFalse(TestGraph.getSei().getServerType().equals(SparqlEndpointInterface.VIRTUOSO_SERVER));
		 
		final String BATTERY = "http://kdl.ge.com/batterydemo#Battery";
		
		// test where all Battery links are out-going
		OntologyInfo oInfo = TestGraph.getOInfo();
		NodeGroup ng1 = new NodeGroup();
		ng1.setSparqlConnection(TestGraph.getSparqlConn());
		Node battNode = ng1.addNode(BATTERY, oInfo);
		ng1.setIsReturned(battNode, true);
		ng1.orderByAll();
		
		Table tab = TestGraph.execQueryToTable(ng1.generateSparqlSelect());
		String instanceUri = tab.getCell(0, 0);
		
		NodeGroup ng = NodeGroup.createConstructAllConnected(
				BATTERY, instanceUri, 
							TestGraph.getSparqlConn(), TestGraph.getOInfo(), TestGraph.getPredicateStats()
							);
		
		Table tab2 = TestGraph.execConstructToNT(ng);
		String res2 = tab2.toCSVString();
		for (String lookup : new String [] {"Cell_cell300", "battA", "1966", "Cell_cell200", "1979"}) {
			assertTrue("Results are missing: " + lookup, res2.contains(lookup));
		}
	}
	
	@Test
	public void testComplexRangeQuery() throws Exception {	
		// WeirdBird has two children, and hasChild has a complex range {Bird, Unusual}
		// A query for just the bird should return just the bird
		SparqlGraphJson sgjson = TestGraph.getSparqlGraphJsonFromResource(this.getClass(), "rangeTestComplexRangeQuery.json");
		Table res = TestGraph.execTableSelect(sgjson);
		assertEquals("Complex range query returned the wrong number of rows", 1, res.getNumRows());
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
	
	private static int countInstances(Table tab) {
		return countAttributes(tab, "#type>", null);
	}
	
	private static int countAttributes(Table tab, String keyContains, String valContains) {
		int ret = 0;
		// loop through the graph array of objects
		for (int i=0; i < tab.getNumRows(); i++) {
			String key = tab.getCell(i, 1);
			String val = tab.getCell(i, 2);
			if (keyContains == null || key.contains(keyContains)   &&  (valContains == null || val.contains(valContains))) {
				ret += 1;
			}
		}
		return ret;
	}

	// Eric & Val: what is "DAG" ?
	@Test
	public void testNodegroupWithDAG() throws Exception {
		String nodeGroupJSONPath = String.format("%s/nodegroup-dag.json", NON_TREE_GRAPHS_DIR);
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, nodeGroupJSONPath);
		Table tab = TestGraph.execQueryToTable(ng.generateSparqlSelect());

		// We expect exactly one result
		assertEquals(1, tab.getNumRows());
	}

	@Test
	public void testNodegroupWithCycle() throws Exception {
		String nodeGroupJSONPath = String.format("%s/nodegroup-cycle.json", NON_TREE_GRAPHS_DIR);
		
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, nodeGroupJSONPath);

		Table tab = TestGraph.execQueryToTable(ng.generateSparqlSelect());

		// We expect exactly two results
		assertEquals(2, tab.getNumRows());
	}

}
