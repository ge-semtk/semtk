/*
 *  ImportTab - the GUI elements for building an ImportSpec
 */
/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
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

define([// properly require.config'ed
        'sparqlgraph/js/datavalidator',
        'sparqlgraph/js/importcsvspec',
        'sparqlgraph/js/importmapping',
        'sparqlgraph/js/mappingitem',
        'sparqlgraph/js/importtrans',
        'sparqlgraph/js/importtext',
        'sparqlgraph/js/importcolumn',

        'jquery',

		// shimmed
		//'logconfig',
		],

	function (DataValidator, ImportCsvSpec, ImportMapping, MappingItem, ImportTransform, ImportText, ImportColumn, $) {

		//============ main object  ImportSpec =============
		var ImportSpec = function (alertCallback) {
			this.alertCallback = (typeof alertCallback === "undefined") ? null: alertCallback;

			this.baseURI = "";
			this.nodegroup = null;
			this.mapList = [];    // ordered list rows
			this.mapHash = {};

            this.dataValidator = new DataValidator([]);
			this.colList = [];    // lists of these objects
			this.textList = [];
			this.transformList = [];
			
		};
		ImportSpec.prototype = {

			addColumn : function (iCol) {
				this.colList.push(iCol);
			},

			addText : function (iText) {
				this.textList.push(iText);
			},

			clearMost : function () {
				// first clear everything
				this.baseURI = "";

				this.mapList = [];
				this.mapHash = {};

				this.colList = [];
				this.textList = [];
				this.transformList = [];

				// then re-apply nodegroup to build blank rows
				this.updateNodegroup(this.nodegroup);
			},

			addTransform : function (iTrans) {
				this.transformList.push(iTrans);
			},

			addRow : function (iMapping) {
				this.mapList.push(iMapping);
				this.mapHash[iMapping.genUniqueKey()] = iMapping;
			},

			delText : function (iText) {
				var index = this.textList.indexOf(iText);
				if (index > -1) {
					this.textList.splice(index, 1);
				}
			},

			delTransform : function (iTransform) {
				var index = this.transformList.indexOf(iTransform);
				if (index > -1) {
					this.transformList.splice(index, 1);
				}
			},

			getBaseURI : function () {
				return this.baseURI;
			},

			setBaseURI : function (uri) {
				this.baseURI = uri;
			},

            getDataValidator : function() {
                return this.dataValidator;
            },

            getNodegroup : function() {
                return this.nodegroup;
            },

			getNumColumns : function () {
				return this.colList.length;
			},

			getSortedColumns : function () {
				// get list of column objects sorted by name
                var ret = this.colList.slice().sort(function(a,b) {
				    if ( a.getColName() < b.getColName() )
				        return -1;
				    if ( a.getColName() > b.getColName() )
				        return 1;
				    return 0;
				} );
				return ret;
			},

			getSortedTexts : function () {
				// get list of column objects sorted by name
				var ret = this.textList.sort(function(a,b) {
				    if ( a.getText() < b.getText() )
				        return -1;
				    if ( a.getText() > b.getText() )
				        return 1;
				    return 0;
				} );
				return ret;
			},

			getSortedTransforms : function () {
				// get list of column objects sorted by name
				var ret = this.transformList.sort(function(a,b) {
				    if ( a.getName() < b.getName() )
				        return -1;
				    if ( a.getName() > b.getName() )
				        return 1;
				    return 0;
				} );
				return ret;
			},

			getSortedMappings : function () {
				// rows are always sorted by node and prop
				return this.mapList;
			},

            getUriSparqlIDs : function () {
                var ret = [];
                for (var i=0; i < this.mapList.length; i++) {
                    if (this.mapList[i].isNode()) {
                        ret.push(this.mapList[i].getName());
                    }
                }
                return ret;
            },

            hasMapping : function(sparqlId, propUri) {
                var key = ImportMapping.staticGenUniqueKey(sparqlId, propUri);
                return (key in this.mapHash) && (this.mapHash[key].getItemList().length > 0);
            },

			getMapping : function(sparqlId, propUri) {
				// find a row
				// propUri may be null
				var key = ImportMapping.staticGenUniqueKey(sparqlId, propUri);
				if (key in this.mapHash) {
					return this.mapHash[key];
				} else {
					kdlLogAndThrow("Internal error in ImportSpec.getMapping(). Can't find row: " + sparqlId + ", " + propUri);
				}
			},

            // get any mappings that have uriLookups containing this mapping
            getLookupMappings : function(mapping) {
                var mapName = mapping.getUriLookupName();
                var ret = [];
                for (var i=0; i < this.mapList.length; i++) {
					if (this.mapList[i].getUriLookupIDs().indexOf(mapName) > -1) {
                        ret.push(this.mapList[i]);
                    }
				}
                return ret;
            },

            // make sure URILookupModes are null iff no other node is looking it up
            updateEmptyUriLookupModes : function() {
                for (var i=0; i < this.mapList.length; i++) {
					var map = this.mapList[i];

					// if row is for a node (not a prop)
					if (map.isNode()) {

                        // set back to null if no one is lookup up this node
                        if (map.getUriLookupMode() != null) {
                            if (this.getLookupMappings(map).length == 0) {
                                map.setUriLookupMode(null);
                            }

                        // set to default if null and someone is looking up this node
                        } else {
                            if (this.getLookupMappings(map).length > 0) {
                                map.setUriLookupMode(ImportMapping.LOOKUP_MODE_NO_CREATE);
                            }
                        }

					}
				}
            },

			fromJson : function(jObj) {
				// load from json.
				// Presume that nodegroup is loaded so rows exist but their items will be overwritten.
				if (this.nodegroup === null) { kdlLogAndThrow("Internal error in ImportSpec.fromJson().  No nodegroup is loaded.");}

				// delete all itemLists
				for (var i=0; i < this.mapList.length; i++) {
					this.mapList[i].clearItems();
				}

				// clear anything that has to do with items
				this.colList = [];
				this.textList = [];
				this.transformList = [];
                this.dataValidator = null;

				// temporary hash for cross referencing JSON things by "id"
				var idHash = {};

				// base URI
				if (jObj.hasOwnProperty("baseURI")) {
					this.baseURI = jObj.baseURI;
				} else {
					this.baseURI = "";
				}

				// columns
				if (! jObj.hasOwnProperty("columns")) { kdlLogAndThrow("Internal error in ImportSpec.fromJson().  No columns field.");}
				for (var i=0; i < jObj.columns.length; i++) {
					var c = new ImportColumn(null);
					var id = c.fromJson(jObj.columns[i]);
					this.addColumn(c);
					idHash[id] = c;
				}

                // dataValidator (it may be null)
                this.dataValidator = new DataValidator(jObj.dataValidator);

				// texts
				if (! jObj.hasOwnProperty("texts")) { kdlLogAndThrow("Internal error in ImportSpec.fromJson().  No texts field.");}
				for (var i=0; i < jObj.texts.length; i++) {
					var t = new ImportText(null);
					var id = t.fromJson(jObj.texts[i]);
					this.addText(t);
					idHash[id] = t;
				}

				// transforms
				if (! jObj.hasOwnProperty("transforms")) { kdlLogAndThrow("Internal error in ImportSpec.fromJson().  No transforms field.");}
				for (var i=0; i < jObj.transforms.length; i++) {
					var t = new ImportTransform(null, null, null, null);
					var id = t.fromJson(jObj.transforms[i]);
					this.addTransform(t);
					idHash[id] = t;
				}

				// nodes
				// Rows should already exist with empty item lists (from updating the nodegroup)
				if (! jObj.hasOwnProperty("nodes")) { kdlLogAndThrow("Internal error in ImportSpec.fromJson().  No nodes field.");}

				for (var i=0; i < jObj.nodes.length; i++) {
					if (! jObj.nodes[i].hasOwnProperty("sparqlID")) { kdlLogAndThrow("Internal error in ImportSpec.fromJson().  Node has no sparqlId.");}

					// find node row and fill it in
					var n = this.getMapping(jObj.nodes[i].sparqlID, null);
					n.fromJsonNode(jObj.nodes[i], idHash, this.nodegroup);
					
					// if node type has subclasses, then there is a sub-type row
					if (gOInfo.getSubclassNames(n.getNode().getURI()).length > 0) {
						var nt = this.getMapping(jObj.nodes[i].sparqlID, ImportMapping.TYPE_URI);
						if (jObj.nodes[i].hasOwnProperty("type_restriction")) {
							nt.fromJsonNode(jObj.nodes[i].type_restriction, idHash, this.nodegroup, n.getNode());
						}
					}

					for (var j=0; j < jObj.nodes[i].props.length; j++) {
						if (! jObj.nodes[i].props[j].hasOwnProperty("URIRelation")) { kdlLogAndThrow("Internal error in ImportSpec.fromJson().  Prop has no URIRelation in node: " + jObj.nodes[i].sparqlID );}

						// find property row and fill it in
						var p = this.getMapping(jObj.nodes[i].sparqlID, jObj.nodes[i].props[j].URIRelation);
						p.fromJsonProp(jObj.nodes[i].props[j], n.getNode(), idHash, this.nodegroup);
					}
				}
			},

			toJson : function(optCompressFlag) {
				var compressFlag = (typeof optCompressFlag == "undefined") ? false : optCompressFlag;

				var ret = {};
				var idHash = {};

				ret.version = "1";

                this.updateEmptyUriLookupModes();

				// baseURI
				ret.baseURI = this.baseURI;

				// columns
				ret.columns = [];
				for (var i=0; i < this.colList.length; i++) {
					var key = "col_" + i;
					idHash[key] = this.colList[i];
					ret.columns.push(this.colList[i].toJson(key));
				}

                ret.dataValidator = this.dataValidator.toJson();

				// text
				ret.texts = [];
				for (var i=0; i < this.textList.length; i++) {
					var key = "text_" + i;
					idHash[key] = this.textList[i];
					ret.texts.push(this.textList[i].toJson(key));
				}

				// transforms
				ret.transforms = [];
				for (var i=0; i < this.transformList.length; i++) {
					var key = "trans_" + i;
					idHash[key] = this.transformList[i];
					ret.transforms.push(this.transformList[i].toJson(key));
				}

				// rows : they must be ordered (Node1, null), (Node1, prop1), (Node1, prop2), (Node2, null), etc.
				ret.nodes = [];
				var lastNodeObj = null;
				for (var i=0; i < this.mapList.length; i++) {
					var map = this.mapList[i];

					// if row is for a node (not a prop)
					if (map.getPropItem() == null) {
						// push the row on as a new node
						lastNodeObj = map.toJson(idHash);
						lastNodeObj.props = [];
						ret.nodes.push(lastNodeObj);
						
 					} else if (map.getPropItem().getURI() == ImportMapping.TYPE_URI) {
						// the _type is stored as a propertyItem only by this importspec while it is displaying
						// change it to node.type_restriction in the json
						if (map.getItemList().length > 0) {
							lastNodeObj.type_restriction = map.toJson(idHash);
						}
					
					} else if (map.getItemList().length > 0 || map.getUriLookupNodes().length > 0) {
						// if row is property and has items
				        lastNodeObj.props.push(map.toJson(idHash));
					}
				}
				return ret;
			},

			// get list of all PropertyItems that have mappings
			getUndeflatablePropItems : function() {
				var ret = [];
				for (var i=0; i < this.mapList.length; i++) {
					var map = this.mapList[i];
					// if row is for a node (not a prop)
					if (map.isProperty()) {
                        if (map.getItemList().length > 0 || map.getUriLookupNodes().length > 0) {
                            var pItem = map.getPropItem();
                            ret.push(pItem);
                        }
					}
				}
				return ret;
			},

            /*
                For a class snode, get a list of properties which are mapped.
            */
            getMappedProperties : function (snode) {
                var ret = [];

                for (var map of this.mapList) {
                    var prop = map.getPropItem();
                    if (map.getNode().getSparqlID() == snode.getSparqlID() && prop != null && prop != ImportMapping.TYPE_URI && map.getItemList().length > 0) {
                        ret.push(prop.getURI());
                    }
                }
                return ret;
            },

			updateNodegroup : function (nodegroup) {
				// Build new mapList and mapHash based on this.nodegroup
				// saving any existing itemLists

				this.nodegroup = nodegroup;

				// save copy of previous state
				var oldRowHash = {};
				for (var k in this.mapHash) {
					oldRowHash[k] = this.mapHash[k];
				}

				// reset rows
				this.mapList = [];
				this.mapHash = {};

				if (this.nodegroup === null) { return; }

				// nested loop through the nodegroup
				var nodeList = this.nodegroup.getOrderedNodeList();
				for (var i = 0; i < nodeList.length; i++) {
					var node = nodeList[i];

					// Build a ImportMapping for the node URI
					var prop = null;
					var map = new ImportMapping(node, prop);
					var key = map.genUniqueKey();
					if (key in oldRowHash) {
						map.itemList = oldRowHash[key].itemList.slice();
                        map.setUriLookupMode(oldRowHash[key].getUriLookupMode());
                        map.setUriLookupNodes(oldRowHash[key].getUriLookupNodes());
						oldRowHash[key] = null;
					}

					this.addRow(map);
					
					if (gOInfo.getSubclassNames(node.getURI()).length > 0) {
						// Build a ImportMapping for the node TYPE
						// The ImportMapping object needs a "fake" propertyItem
						var prop = new PropertyItem("uri", "class", ImportMapping.TYPE_URI);
						prop.setSparqlID(node.getBindingOrSparqlID() + "_type" + ImportMapping.ID_TYPE_SUFFIX);
	
						var map = new ImportMapping(node, prop);
						var key = map.genUniqueKey();
						if (key in oldRowHash) {
							map.itemList = oldRowHash[key].itemList.slice();
	                        map.setUriLookupMode(oldRowHash[key].getUriLookupMode());
	                        map.setUriLookupNodes(oldRowHash[key].getUriLookupNodes());
							oldRowHash[key] = null;
						}
	
						this.addRow(map);
					}

					// Build a ImportMapping for each property
					var propItemList = nodeList[i].propList;
					if (propItemList) {
						for (var j=0; j < propItemList.length; j++) {
							prop = propItemList[j];
							map = new ImportMapping(node, prop);
							var keyP = map.genUniqueKey();
							if (keyP in oldRowHash) {
								map.itemList = oldRowHash[keyP].itemList.slice();
                                map.setUriLookupNodes(oldRowHash[keyP].getUriLookupNodes());
								oldRowHash[keyP] = null;
							}

							this.addRow(map);
						}
					}
				}
			},

			/**
			 * Alert if there's a callback, otherwise log to console
			 * @param: {string} msg the message
			 */
			alert : function(msg) {
				if (this.alertCallback !== null) {
					this.alertCallback(msg);
				} else {
					console.log("Importspec alert: " + msg);
				}
			},

			updateColsFromCsvAsync : function (csvFile, callback) {
				this.importCsvSpec = new ImportCsvSpec();

				var fullCallback = function(x) {
										this.updateColsFromCsvCallback();
										callback();
									}.bind(this);

				this.importCsvSpec.loadCsvFileAsync(csvFile, fullCallback);

			},

			updateColsFromCsvCallback : function () {
				// looks at a (new) importCSVSpec for new column names
				// 	- keeps any existing column MappingItems that have value pointing to valid column
				// 	- deletes any MappingItems that point to non-existing columns
				// 	- adds any new columns
				//


				//             GUI must redraw the available columns
				var newColNames = this.importCsvSpec.getColNames();


				// loop through all existing items
				for (var i=0; i < this.mapList.length; i++) {
					var itemList = this.mapList[i].getItemList();
					for (var j=0; j < itemList.length; j++) {
						var item = itemList[j];

						// find the columns in items
						if (item.getType() == MappingItem.TYPE_COLUMN) {
							var col = item.getColumnObj();

							// if column is still valid
							if (newColNames.indexOf(col.getColName()) < 0) {
								// delete the item
								this.mapList[i].delItem(item);
							}
						}
					}
				}

				var keepColNames = [];

				// delete any invalid cols
				for (var i=0; i < this.colList.length; i++) {
					if (newColNames.indexOf(this.colList[i].getColName()) < 0) {
						this.colList.splice(i, 1);
					} else {
						keepColNames.push(this.colList[i].getColName());
					}
				}

				// add any new columns not already represented
				for (var i=0; i < newColNames.length; i++) {
					// if column isn't already being kept
					if (keepColNames.indexOf(newColNames[i]) < 0) {
						var c = new ImportColumn(newColNames[i]);
						this.addColumn(c);
					}
				}
			},

        };
        return ImportSpec;
	}
);
