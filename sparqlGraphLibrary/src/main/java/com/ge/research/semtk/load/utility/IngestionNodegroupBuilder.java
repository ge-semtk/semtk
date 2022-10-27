package com.ge.research.semtk.load.utility;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 ** Copyright 2021 General Electric Company
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
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.XSDSupportedType;

/**
 * Builds an ingestion template (RACK "CDR") for a particular class based on oInfo.
 * 
 * idRegex may be set which defines ID fields, used for lookup
 * 
 * Default behaviors:
 * 	  - removes "null" or "Null" or "NULL" in input cell
 *    - all properties and node connections are set to OPTIONAL, so nodegroup can also be used to pull data
 *    - all class nodes are either looked up or created with a GUID-based URI
 *    - property name that matches <idRegex> will be used to lookup it's parent node in CreateIfMissing mode
 *    - object properties must have a property that matches <idRegex> in order to be created or linked
 *    		- if range has subclasses is noCreate (errorIfMissing) mode
 *    		- otherwise createIfMissing
 *    - ingestion columns:  properties of the main class and <idRegex> properties of linked objects
 *          - properties of main class column is named propKeyName
 *    		- idRegex property of linked object column is named linkPropKeyname_propKeyName
 *          - columns have these patterns removed from key names:  "^is", "^is-", and "^has"
 *    
 * @author 200001934
 *
 */
public class IngestionNodegroupBuilder {
	private String className;
	private SparqlConnection conn;
	private OntologyInfo oInfo;
	private SparqlGraphJson sgjson;
	private StringBuilder csvTemplate;
	private StringBuilder csvTypes;
	private String idRegex = null;
	
	/**
	 * 
	 * @param className
	 * @param conn - where to ingest data
	 * @param oInfo - already loaded from conn 
	 * 
	 */
	public IngestionNodegroupBuilder(String className, SparqlConnection conn, OntologyInfo oInfo) {
		this.className = className;
		this.conn = conn;
		this.oInfo = oInfo;
	}
		
	/**
	 * Regex that matches entire property keyname for lookup properties
	 * @param val
	 */
	public void setIdRegex(String val) {
		this.idRegex = val;
	}
	
	public String getNodegroupJsonStr() {
		return this.sgjson.prettyPrint();
	}
	
	public SparqlGraphJson getSgjson() {
		return this.sgjson;
	}
	
	/**
	 * Get a string of "colname1, colname2, ..."
	 * @return
	 */
	public String getCsvTemplate() {
		return this.csvTemplate.toString();
	}
	
	/**
	 * Get a string of types that go along with the csv template column names
	 * Also comma separated
	 * If a property can have multiple types they will be space-separated
	 * Short types are used (XSDSupportedTypes simple lower camelcase names like "integer" or "dateTime") 
	 * @return
	 */
	public String getCsvTypes() {
		return this.csvTypes.toString();
	}
	
	public void build() throws Exception {
		this.csvTemplate = new StringBuilder();
		this.csvTypes = new StringBuilder();
		ImportSpec ispecBuilder = new ImportSpec();
		
		// create nodegroup with single node of stated type
		NodeGroup nodegroup = new NodeGroup();
		nodegroup.addNode(this.className, this.oInfo);
		
		// build a rm_null transform
		String transformId = ispecBuilder.addTransform("rm_null", "replaceAll", "^(null|Null|NULL)$", "");
				
		Node node = nodegroup.getNode(0);
		ispecBuilder.addNode(node.getSparqlID(), node.getUri(), null);
		
		// if there are sub-types then add that
		if (this.oInfo.getSubclassNames(node.getUri()).size() > 0) {
			String colName = node.getTypeSparqlID().substring(1);  // type sparqlID without ?
			node.setIsTypeReturned(true);
			ispecBuilder.addTypeRestriction(node.getSparqlID());
			ispecBuilder.addColumn(colName);
			ispecBuilder.addMappingToTypeRestriction(node.getSparqlID(), ispecBuilder.buildMappingWithCol(colName, new String [] {transformId}));
			if (this.idRegex != null &&  Pattern.compile(this.idRegex).matcher(colName).find()) {
				ispecBuilder.addURILookupToTypeRestriction(node.getSparqlID(), node.getSparqlID());
				ispecBuilder.addLookupMode(node.getSparqlID(), ImportSpec.LOOKUP_MODE_CREATE_IF_MISSING);
			}
		}
		
		
		// set all data properties to returned, ensuring they get sparqlIDs
		for (PropertyItem pItem : node.getPropertyItems()) {
			// add to nodegroup return / optional
			nodegroup.setIsReturned(pItem, true);
			
			// add to import spec
			ispecBuilder.addProp(node.getSparqlID(), pItem.getUriRelationship());
			
			if (this.idRegex != null &&  Pattern.compile(this.idRegex).matcher(pItem.getKeyName()).find()) {
				// lookup ID is a lookup and is NOT optional
				ispecBuilder.addURILookup(node.getSparqlID(), pItem.getUriRelationship(), node.getSparqlID());
				ispecBuilder.addLookupMode(node.getSparqlID(), ImportSpec.LOOKUP_MODE_CREATE_IF_MISSING);
			} else {
				// normal properties ARE optional
				pItem.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
			}
			
			String colName = buildColName(pItem.getSparqlID());
			ispecBuilder.addColumn(colName);
			ispecBuilder.addMapping(node.getSparqlID(), pItem.getUriRelationship(), ispecBuilder.buildMappingWithCol(colName, new String [] {transformId}));
			
			// add to csvTemplate
			csvTemplate.append(colName + ",");		
			csvTypes.append(pItem.getValueTypesString(" ") + ",");     
		}
		
		// connect a node for each object property
		for (NodeItem nItem : node.getNodeItemList()) {
			
			for (String rangeUri : nItem.getRangeUris()) {
				// Add object property node to nodegroup (optional) and importSpec
				String rangeKeyname =  new OntologyName(rangeUri).getLocalName();
				Node objNode = nodegroup.addNode(rangeUri, node, null, nItem.getUriConnectBy());
				nItem.setOptionalMinus(objNode, NodeItem.OPTIONAL_TRUE);
				
				ispecBuilder.addNode(objNode.getSparqlID(), objNode.getUri(), ImportSpec.LOOKUP_MODE_ERR_IF_MISSING);

//  we might want to re-add this for a different "flavor" of auto-generated nodegroups
//
//				if (oInfo.hasSubclass(className)) {
//					// If node has subclasses then NO_CREATE ("error if missing")
//					// This will create the need for ingestion order to matter:  linked items must be ingested first.
//					ispecBuilder.addNode(objNode.getSparqlID(), objNode.getUri(), ImportSpec.LOOKUP_MODE_NO_CREATE);
//				} else {
//					// If node has NO subclasses then we may create it.
//					ispecBuilder.addNode(objNode.getSparqlID(), objNode.getUri(), ImportSpec.LOOKUP_MODE_CREATE);
//				}
				
				// give it a name, e.g.: verifies_ENTITY
				String objNodeName = nItem.getKeyName() + "_" + rangeKeyname;
				nodegroup.setBinding(objNode, objNodeName);
				
				// set data property matching ID_REGEX returned
				for (PropertyItem pItem : objNode.getPropertyItems()) {
					if (this.idRegex != null &&  Pattern.compile(this.idRegex).matcher(pItem.getKeyName()).find()) {
						// set the lookup ID to be returned
						// but not optional (link to node is optional instead)
						nodegroup.setIsReturned(pItem, true);
						
						// give ID_REGEX property a meaningful sparqlID
						String sparqlID;
						if (nItem.getRangeUris().size() > 1) {
							// complex range: include the class
							sparqlID = nItem.getKeyName() + "_" + rangeKeyname + "_" + pItem.getKeyName();
							
						} else {
							// 'default'
							sparqlID = nItem.getKeyName() + "_" + pItem.getKeyName();
						}
						String propId = nodegroup.changeSparqlID(pItem, sparqlID);
						
						
						
						// add to importspec, using it to look up parent node
						ispecBuilder.addProp(objNode.getSparqlID(), pItem.getUriRelationship());
						ispecBuilder.addURILookup(objNode.getSparqlID(), pItem.getUriRelationship(), objNode.getSparqlID());
						
						// add the column and mapping to the importspec
						String colName = buildColName(propId);
						ispecBuilder.addColumn(colName);
						ispecBuilder.addMapping(objNode.getSparqlID(), pItem.getUriRelationship(), ispecBuilder.buildMappingWithCol(colName, new String [] {transformId}));
						
						// add to csvTemplate and csvTypes
						csvTemplate.append(colName + ",");
						csvTypes.append(pItem.getValueTypesString(" ") + ","); 
						break;
					}
				}
			}
		}
		
		// set up the SparqlGraphJson
		this.sgjson = new SparqlGraphJson(nodegroup, conn);
		this.sgjson.setImportSpecJson(ispecBuilder.toJson());
		
		// replace last comma in csvTemplate with a line return
		csvTemplate.setLength(Math.max(0,csvTemplate.length()-1));
		csvTemplate.append("\n");
		csvTypes.setLength(Math.max(0,csvTypes.length()-1));
		csvTypes.append("\n");
		
	}
	
	private static String buildColName(String sparqlIdSuggestion) {
		return sparqlIdSuggestion.replace("?", "");
	}

}
