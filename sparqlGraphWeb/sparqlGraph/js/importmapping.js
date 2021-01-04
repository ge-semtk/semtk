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

define([	// properly require.config'ed
        	 'sparqlgraph/js/mappingitem',

     		// shimmed
        	// 'logconfig',
		],

		function (MappingItem) {

			/**
			 * An import mapping from columns, texts, and transforms to a class or property in the nodegroup.
		     * @description <font color="red">Users of {@link SemtkAPI} should not call this constructor.</font><br>Use {@link SemtkImportAPI#getMappings} or {@link SemtkImportAPI#getMapping} instead
			 * @alias ImportMapping
			 * @class
			 */
			var ImportMapping = function (node, propItem, optUriLookupNodes, optUriLookupMode) {
				// params are purposely not documented for jsdoc.  Not designed for the API.
				// node: SemanticNode
				// propItem: PropertyItem
				this.node = node;
				this.propItem = propItem;  // could be null if this is a node
                this.uriLookupNodes = typeof optUriLookupNodes == "undefined" ? []   : optUriLookupNodes;
				this.uriLookupMode =  typeof optUriLookupMode == "undefined"  ? null : optUriLookupMode;
                this.itemList = [];
			};

            // Every Json key needs to have a constant like in the java
            // and I believe they should all be moved to sparqlGraphJson
            ImportMapping.JKEY_IS_NODE_LOOKUP_MODE = "URILookupMode";
            ImportMapping.JKEY_IS_URI_LOOKUP =       "URILookup";

            ImportMapping.LOOKUP_MODE_NO_CREATE = "noCreate";
            ImportMapping.LOOKUP_MODE_CREATE = "createIfMissing";
            ImportMapping.LOOKUP_MODE_ERR_IF_EXISTS = "errorIfExists";

            ImportMapping.LOOKUP_MODE_LIST = [
                ImportMapping.LOOKUP_MODE_NO_CREATE,
                ImportMapping.LOOKUP_MODE_CREATE,
                ImportMapping.LOOKUP_MODE_ERR_IF_EXISTS
            ]

			ImportMapping.staticGenUniqueKey = function(sparqlId, propUri) {
				return sparqlId + "." + (propUri != null ? propUri : "");
			};

			ImportMapping.prototype = {
				//
				// NOTE any methods without jsdoc comments is NOT meant to be used by API users.
				//      These methods' behaviors are not guaranteed to be stable in future releases.
				//

				/**
				 * @description Add a mapping item
				 * @param {MappingItem} item           item to add
				 * @param {MappingItem} insertBefore   insert here.  If null then add to end.
				 */
				addItem : function (item, insertBefore) {
					if (insertBefore == null) {
						this.itemList.push(item);
					} else {
						var pos = this.itemList.indexOf(insertBefore);
						this.itemList.splice(pos, 0, item);
					}
					item.incrUse(1);
				},

				/**
				 * @description clears all mapping items
				 */
				clearItems : function () {
					// clears items without any regard for the ImportSpec
					this.itemList = [];
				},

				/**
				 * @description delete a given mapping item
				 * @param {MappingItem} item the mapping item to delete
				 * @throws exception if item is invalid
				 */
				delItem : function (item) {
					var i = this.itemList.indexOf(item);
					if (i < 0) { kdlLogAndThrow("Internal error in ImportRow.delItem().  Item isn't in this row.");}

					item.incrUse(-1);
					this.itemList.splice(i, 1);
				},

				fromJsonNode : function (jNode, idHash, ng) {
					if (! jNode.hasOwnProperty("sparqlID")) { kdlLogAndThrow("Internal error in ImportRow.fromJsonNode().  No sparqlID field.");}

					var node = ng.getNodeBySparqlID(jNode.sparqlID);
					if (! node) {kdlLogAndThrow("ImportMapping.fromJsonNode() can't find node in nodegroup: " + jNode.sparqlID); }
					this.node = node;
					this.propItem = null;

					if (! jNode.hasOwnProperty("mapping")) {
                        kdlLogAndThrow("Internal error in ImportRow.fromJsonNode().  No mapping field.");
                    }
					this.itemsFromJson(jNode.mapping, idHash);

                    if (jNode.hasOwnProperty(ImportMapping.JKEY_IS_NODE_LOOKUP_MODE)) {
                        if (jNode[ImportMapping.JKEY_IS_NODE_LOOKUP_MODE] == ImportMapping.LOOKUP_MODE_CREATE ||
                            jNode[ImportMapping.JKEY_IS_NODE_LOOKUP_MODE] == ImportMapping.LOOKUP_MODE_NO_CREATE ||
                            jNode[ImportMapping.JKEY_IS_NODE_LOOKUP_MODE] == ImportMapping.LOOKUP_MODE_ERR_IF_EXISTS) {
                            this.uriLookupMode = jNode[ImportMapping.JKEY_IS_NODE_LOOKUP_MODE];
                        } else {
                            kdlLogAndThrow("Invalid URILookupMode in import spec: " + jNode[ImportMapping.JKEY_IS_NODE_LOOKUP_MODE]);
                        }
                    } else {
                        this.uriLookupMode = null;
                    }

                    this.fromJsonUriLookup(jNode, ng);
				},

				fromJsonProp : function (jProp, node, idHash, ng) {
					this.node = node;

					if (! jProp.hasOwnProperty("URIRelation")) { kdlLogAndThrow("Internal error in ImportRow.fromJsonProp().  No URIRelation field.");}
					var propItem = node.getPropertyByURIRelation(jProp.URIRelation);
					if (! propItem) { kdlLogAndThrow("ImportMapping.fromJsonProp() can't find property in nodegroup: " + node.getSparqlID() + "->" + jProp.URIRelation); }

					if (! jProp.hasOwnProperty("mapping")) { kdlLogAndThrow("Internal error in ImportRow.fromJsonProp().  No mapping field.");}
					this.itemsFromJson(jProp.mapping, idHash);

                    this.fromJsonUriLookup(jProp, ng);
				},

                fromJsonUriLookup : function (jObj, ng) {
                    if (jObj.hasOwnProperty(ImportMapping.JKEY_IS_URI_LOOKUP)) {
                        var sparqlIDs = jObj[ImportMapping.JKEY_IS_URI_LOOKUP];
                        for (var i=0; i < sparqlIDs.length; i++) {
                            var snode = ng.getNodeBySparqlID(sparqlIDs[i]);
                            if (snode == null) {
                                kdlLogAndThrow("Invalid URILookup sparqlID in import spec: " + sparqlIDs[i]);
                            } else {
                                this.uriLookupNodes.push(snode);
                            }
                        }
                    }
                },

                getUriLookupNodes : function() {
                    return this.uriLookupNodes;
                },

                getUriLookupIDs : function () {
                    var ret = [];
                    for (var i=0; i < this.uriLookupNodes.length; i++) {
                        ret.push(this.uriLookupNodes[i].getSparqlID());
                    }
                    return ret;
                },

                // snodeList may be []
                setUriLookupNodes : function(snodeList) {
                    this.uriLookupNodes = snodeList;
                },

                // returns a URILookupMode or null
                getUriLookupMode : function() {
                    return this.uriLookupMode;
                },

                setUriLookupMode : function(modeStr) {
                    this.uriLookupMode = modeStr;
                },


				getNode : function () {
					return this.node;
				},

				getPropItem : function () {
					return this.propItem;
				},

				/**
				 * @description Get the MappingItems for this ImportMapping
				 * @returns {MappingItem[]} ordered list of items
				 */
				getItemList : function () {
					return this.itemList;
				},

				genUniqueKey : function () {
					// key is unique to a nodegroup
					return ImportMapping.staticGenUniqueKey(this.getNodeSparqlId(), this.getPropUri());
				},

                // get the name that would match a URI LOOKUP
                // e.g. nodes are looked up by sparqlID
                getUriLookupName : function () {
                    if (this.isNode()) {
                        return this.node.getSparqlID();
                    } else {
                        return this.propItem.getKeyName();
                    }
                },

                // get the display name
                // e.g. bindings are displayed where available
                getName : function () {
                    if (this.isNode()) {
                        return this.node.getBindingOrSparqlID();
                    } else {
                        return this.propItem.getBindingOrSparqlID() || this.propItem.getKeyName();
                    }
                },

                isProperty : function() {
                    return (this.propItem != null);
                },

                isNode : function() {
                    return ! this.isProperty();
                },

				itemsFromJson : function (jMapping, idHash) {
					for (var i=0; i < jMapping.length; i++) {
						var item = new  MappingItem(null, null, null);
						item.fromJson(jMapping[i], idHash);
						this.itemList.push(item);
					}
				},

				toJson : function (idHash) {
					var ret = {};
					if (this.propItem == null) {
						// do node row
						ret.sparqlID = this.node.getSparqlID();
						ret.type = this.node.getURI();
					} else {
						// prop row
						ret.URIRelation = this.propItem.getUriRelation();
					}

                    // URILookupMode
                    if (this.uriLookupMode != null) {
                        ret.URILookupMode = this.uriLookupMode;
                    }

                    if (this.uriLookupNodes.length != 0) {
                        ret.URILookup = this.getUriLookupIDs();
                    }

					//mapping
					ret.mapping = [];
					for (var i=0; i < this.itemList.length; i++) {
						ret.mapping.push(this.itemList[i].toJson(idHash));
					}

					return ret;
				},

				getNodeSparqlId : function() {
					return this.node.getSparqlID();
				},

				getPropUri : function() {
					return this.propItem ? this.propItem.getUriRelation() : null;
				},



			};
			return ImportMapping;
	}
);
