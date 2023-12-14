define([	// properly require.config'ed
            'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/iidxhelper',

        	'jquery',

            'visjs/vis.min'
			// shimmed

		],
        function(ModalIidx, IIDXHelper, $, vis) {

	var ADD_TRIPLES_WARN = 100;     // when adding triples to graph, warn if this many
	var ADD_TRIPLES_MAX = 1000;     // when adding triples to graph, disallow more than this many

    var VisJsHelper = function () {
    };

	VisJsHelper.BLANK_NODE = "blank_node";
	VisJsHelper.DATA_NODE = "data";
	VisJsHelper.BLANK_NODE_REGEX ="^(nodeID://|_:)";

	/**
	 * Show/remove wait cursor for network
	 */
    VisJsHelper.networkBusy = function(canvasDiv, flag) {
        var canvas = canvasDiv.getElementsByTagName("canvas")[0];
        canvas.style.cursor = (flag ? "wait" : "");
    };
		
    VisJsHelper.createCanvasDiv = function(id) {
        var canvasdiv = document.createElement("div");
        canvasdiv.id= id;
        canvasdiv.style.height="100%";
        canvasdiv.style.width="100%";

        return canvasdiv;
    };

    VisJsHelper.createConfigDiv = function(id) {
        var configdiv = document.createElement("div");
        configdiv.style.margin="1ch";
        configdiv.id= id;
        configdiv.style.display="table";
        configdiv.style.background = "rgba(32, 16, 16, 0.2)";
        return configdiv;
    };

    VisJsHelper.showConfigDialog = function(configdiv, saveConfigCallback) {

        // hack at getting the UI colors so they don't look terrible
        for (var e of configdiv.children) {
            e.style.backgroundColor='white';
            for (var ee of e.children) {
                if (! ee.innerHTML.startsWith("generate")) {
                    ee.style.backgroundColor='white';
                }
            }
        }

        var m = new ModalIidx("ModalIidxAlert");
        m.showOK("Network physics", configdiv, saveConfigCallback);
    };

    VisJsHelper.getDefaultOptions = function(configdiv) {
        return {
            configure: {
                enabled: true,
                container: configdiv,
                filter: "layout physics",
                showButton: true
            },
            groups: {
                useDefaultGroups: true,
                data: {color:{background:'white'}, shape: 'box'},
                blank_node: {color:{background:'white', border:'red'}, shape: 'box'}
            },
            interaction: {
                multiselect: true,
                navigationButtons: true,
                keyboard: {
                    bindToWindow: false
                }
            },
            manipulation: {
                initiallyActive: false,
                deleteNode: true,
                deleteEdge: true,
            }
        };
    };

    VisJsHelper.setCustomEditingOptions = function(options) {
        options.manipulation.enabled = false;    // turn off built-in editing
        return options;
    };

    VisJsHelper.buildCustomEditDOM = function() {
        var span = document.createElement("span");
        var but;

        but = IIDXHelper.createIconButton("icon-remove", function(){ alert("delete");}, undefined, undefined, "Delete", "click to delete" );
        span.appendChild(but);

        but = IIDXHelper.createIconButton("icon-plus", function(){ alert("add");}, undefined, undefined, "Add", "click to add" );
        span.appendChild(but);

        return span;
    };

    // get the shortened "local" readable type name(s)
    //
    // object either has no type: "untyped"
    // or has a single type
    // or has an array of types
    VisJsHelper.getShortType = function(j) {
        var typ = j["@type"] || "prefix#untyped";

        // TODO: instead, we should be honoring fuseki's @prefix fields
        // if there's no # and there is : then replace the last : with #
        if (typ.indexOf("#") == -1 && typ.indexOf(":") > -1) {
            var pos = typ.lastIndexOf(":");
            typ = typ.slice(0,pos) + "#" + typ.slice(pos+1);
        }

        if (Array.isArray(typ)) {
            ret = "";
            for (var i=0; i < typ.length; i++ ) {
                // concatenate short values
                ret += "," + ((typ[i].indexOf("#") > -1) ? typ[i].split("#")[1] : typ[i])
            }
            ret = ret.slice(1);
        } else {
            ret = ((typ.indexOf("#") > -1) ? typ.split("#")[1] : typ);
        }
        return ret;
    };

    // get the full type name, not including any context expansion
    // concatenates multiple types into a string
    VisJsHelper.getLongType = function(j) {
        var typ = j["@type"] || "untyped";
        if (Array.isArray(typ)) {
            ret = "";
            for (var i=0; i < typ.length; i++ ) {
                // concatenate entire values
                ret += "," + typ[i]
            }
            ret = ret.slice(1);
        } else {
            ret = typ;
        }
        return ret;
    };

	// optLabelIdFlag - instead of "normal formatting", Label nodes with the id (URI)
    VisJsHelper.addJsonLdObject = function(j, nodeDict, edgeList, optLabelIdFlag) {

        // add the main node
        var shortType = VisJsHelper.getShortType(j);
        var groupVal = VisJsHelper.getLongType(j);
        if (groupVal.indexOf("XMLSchema") > -1) {
            groupVal = VisJsHelper.DATA_NODE;
        }

        // add node to nodeDict, potentially overwriting any place-holder
        var id = j["@id"];
        nodeDict[id] = {
            id : id,
            label: optLabelIdFlag ? id : shortType,
            title: optLabelIdFlag ? shortType : id,
            group: groupVal
        };

        //console.log(groupVal);
        // handle slightly buggy behavior in visJs:
        // if potentially changing the node's group from a known group back to a useDefaultGroups
        // (this happens if node was originally a data property but now we run across it in the JSON-LD)
        // then reset the shape manually or visJs will leave it custom.
        if (groupVal != VisJsHelper.DATA_NODE) {
            nodeDict[id].shape = undefined;
        }

        // loop through properties
        for (var key in j) {

            // if it isn't a special node property starting with "@"
            if (key[0] != "@") {
                // get the predicate name
                var predName = (key.indexOf("#") > -1) ? key.split("#")[1] : key;

                // make objects into an array
                var objects;
                if (Array.isArray(j[key])) {
                    objects = j[key];
                } else if (j[key].hasOwnProperty("@list") ) {
                    objects = j[key]["@list"];
                } else {
                    objects = [ j[key] ];
                }

                // for each object
                for (o of objects) {
                    // is the object a node or just a piece of data
                    // virutoso shows nodes as {@id: 2342A}
                    // but fuseki just uses a string "2342A" which looks exactly like a string data property

                    var surelyNodeFlag = o.hasOwnProperty("@id");   // detect virtuoso node
                    surelyNodeFlag = surelyNodeFlag || (typeof o == "string" && nodeDict.hasOwnProperty(o));  // detect node already added

                    if (surelyNodeFlag) {
                        var objectId = o.hasOwnProperty("@id") ? o["@id"] : o;
                        if (typeof objectId !== "string") {
                            throw "VisJsHelper can not determine object of predicate " + predName + " found " + o;
                        }
                        // add edge
                        //console.log(j["@id"]+"-"+predName+"-"+objectId);

                        edgeList.push({
                            id: j["@id"]+"-"+predName+"-"+objectId,  // prevent duplicate edges
                            from: j["@id"],
                            to: objectId,
                            label: predName,
                            arrows: 'to',
                            color: {inherit: false},
                            group: key
                        });
                    } else {
                        // either a node that hasn't been added yet, or a data property.

                        // get value and type:
                        // sometimes { @value: 35, @type: integer } and sometimes just "35" or 35
                        var val = o.hasOwnProperty("@value") ? o["@value"] : o.toString();
                        var id;
                        var typ;
						var grp;
						
						// If we're not in special lablIdFlag mode AND blank node has slipped in as an object
						// stop exploration here
						// (blank nodes are not consistent across queries)
						if (!optLabelIdFlag && val.match(VisJsHelper.BLANK_NODE_REGEX)) {
							grp = VisJsHelper.BLANK_NODE;
							val = VisJsHelper.BLANK_NODE
							id = j['@id'] + "_" + key + "_blank";
							typ = VisJsHelper.BLANK_NODE;

						} else {
							grp = VisJsHelper.DATA_NODE;
							id = val;
							typ  = VisJsHelper.getLongType(o);
						}
						
                        // add data property node
                        // if it later turns out that this is a node (fuseki's JSON_LD format)
                        // it will be overwritten later at the logic above "add node to nodeDict"
                        nodeDict[val] = {
                            id : id,
                            label: val,
                            title: typ,
                            group: grp
                        };

                        // add the edge
                        //console.log(j["@id"]+"-"+predName+"-"+val);
                        edgeList.push({
                            id: j["@id"]+"-"+predName+"-"+val,   // prevent duplicate edges
                            from: j["@id"],
                            to: id,
                            label: predName,
                            arrows: 'to',
                            color: {inherit: false},
                            group: key
                        });
                    }
                }
            }
        }
    };

    /**
	 * Expand the selected nodes in a graph
	 * canvasDiv - the canvas div containing the network
	 * network - the network
	 */
    VisJsHelper.expandSelectedNodes = function(canvasDiv, network, optNodeLabelProp) {
        require(['sparqlgraph/js/modaliidx', 'sparqlgraph/js/msiclientnodegroupexec'
			    ], function(ModalIidx, MsiClientNodeGroupExec) {

            VisJsHelper.networkBusy(canvasDiv, true);
            var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.longTimeoutMsec);
            var resultsCallback = MsiClientNodeGroupExec.buildJsonLdOrTriplesCallback(
                VisJsHelper.addTriples.bind(this, canvasDiv, network, optNodeLabelProp?optNodeLabelProp:""), // add triples to graph
                networkFailureCallback.bind(this, canvasDiv),
                function() {}, // no status updates
                function() {}, // no check for cancel
                g.service.status.url,
                g.service.results.url);
            var idList = network.getSelectedNodes();
            if(idList.length > 1){
				// when expanding, we show a dialog with choices about what to load for a given node (esp if over the triple limit).  Limit to expanding one node at a time.
				ModalIidx.alert("Select a single node", "Cannot expand multiple nodes at once.");
				VisJsHelper.networkBusy(canvasDiv, false);
				return;
			}
            for (var id of idList) {
                var classUri = network.body.data.nodes.get(id).group;
             	if (classUri == VisJsHelper.BLANK_NODE) {
					VisJsHelper.networkBusy(canvasDiv, false);
					ModalIidx.alert("Blank node error", "Can not expand a blank node returned from a previous query.")
				} else if (classUri == VisJsHelper.DATA_NODE) {
                    var instanceUri = id;
                    client.execAsyncConstructConnectedData(instanceUri, null, null, SemanticNodeGroup.RT_NTRIPLES, ADD_TRIPLES_MAX, gConn, resultsCallback, networkFailureCallback.bind(this, canvasDiv));
                } else {
                    // get classname and instance name with ':' prefix expanded out to full '#' uri
                    var instanceUri = id;
                    client.execAsyncConstructConnectedData(instanceUri, "node_uri", 
                    	optNodeLabelProp?[optNodeLabelProp]:[],
                    	SemanticNodeGroup.RT_NTRIPLES, ADD_TRIPLES_MAX, gConn, resultsCallback, networkFailureCallback.bind(this, canvasDiv));
                }
            }
        });
    };

	/**
	 * Add triples to a graph.  
	 * If triples exceed warn/max threshold(s), allow user to load subset or full set.
	 * 
	 * canvasDiv - the canvas div containing the network
	 * network - the network
	 * triplesRes - a results object containing n-triples
	 */
	VisJsHelper.addTriples = function(canvasDiv, network, nodeLabelProperty, triplesRes) {
	
		var nodeDict = {};   // dictionary of nodes with @id as the key
		var edgeList = [];

		if (triplesRes.isNtriplesResults()) {

			// Load function for after we've tested size and chopped it down if needed
			var loadTriples = function(nodeDict, edgeList, canvasDiv, network, triples) {
				for (var i = 0; i < triples.length; i++) {
					VisJsHelper.addTripleToDicts(network, triples[i], nodeDict, edgeList, false, true, nodeLabelProperty);
				}
				network.body.data.nodes.update(Object.values(nodeDict));
				network.body.data.edges.update(edgeList);
				VisJsHelper.networkBusy(canvasDiv, false);
			}.bind(this, nodeDict, edgeList, canvasDiv, network);
		
			// identify the triples that match the selected predicate(s) and display them
			var displayTriplesWithSelectedPredicates = function(selectedPredicates) {  // selectedPredicates may be a string (if only 1 predicate selected) or a list of strings
				var triplesSubset = [];
				var selectedSubjectsAndObjects = [];
				// pass 1: get triples with the selected predicate(s).  Also gather a list of subjects and objects for these triples.
				for (var i = 0; i < triples.length; i++) {
					if(triples[i][1] == selectedPredicates || selectedPredicates.includes(triples[i][1])){
						triplesSubset.push(triples[i]);
						selectedSubjectsAndObjects.push(triples[i][0]); // remember the subject, will need type for it
						selectedSubjectsAndObjects.push(triples[i][2]);	// remember the object, will need type for it
					}
				}
				// pass 2: add types for subjects/objects that will be displayed
				for (var i = 0; i < triples.length; i++) {
					if((selectedSubjectsAndObjects.includes(triples[i][0]) || selectedSubjectsAndObjects.includes(triples[i][2]))  && VisJsHelper.isTypePredicate(triples[i][1])){
						triplesSubset.push(triples[i]);
					}
				}
				// display the triples
				loadTriples(triplesSubset);
			}
			
			// show a dialog where the user can choose predicate(s).  Dialog shows list of predicates and # triples for each
			showPredicateChoiceDialog = function(triples){
				// get counts
				var p = null;
				predicateCountHash = {};
				for (var i = 0; i < triples.length; i++) {
					p = triples[i][1];
					if(predicateCountHash[p] == null){
						predicateCountHash[p] = 0;
					}
					predicateCountHash[p]++;
				}	
				// display predicates/counts in a dialog
				var predicateChoiceList = [];
				for (const [pred, count] of Object.entries(predicateCountHash)) {
					if(!VisJsHelper.isTypePredicate(pred)){
				 		predicateChoiceList.push([pred + " (" + count + " connections)", pred]);
				 	}
				 	predicateChoiceList.sort();
				}
				ModalIidx.multiListDialog("Select predicates:",
					"OK",
					predicateChoiceList, [],
					displayTriplesWithSelectedPredicates
				);
			}

			triples = triplesRes.getNtriplesArray();
			if (triples.length < ADD_TRIPLES_WARN) {
				// considered giving option to downselect by predicate here, but makes certain operations clumsy (e.g. clicking SHACL instance checkbox)
				loadTriples(triples);

			} else if (triples.length < ADD_TRIPLES_MAX) {
				// "Warning zone" : user can load none, all, or downselect by predicate
				ModalIidx.choose("Large number of nodes returned",
					triples.length + " nodes and edges were returned. This could overload your browser.<br>",
					["load all " + triples.length, 
					 "choose predicates to load", 
					 "cancel"],
					[function() { loadTriples(triples); },
					function() { showPredicateChoiceDialog(triples); },
					function() { }]
				);
			} else {
				// "Danger zone": All were not returned so they can't be loaded. User can load none, the returned subset, or the returned subset downselected by predicate. 
				triples = triples.slice(0, ADD_TRIPLES_MAX)  // truncate further since back-end can overshoot
				ModalIidx.choose("Large number of nodes returned",
					"Over " + ADD_TRIPLES_MAX + " nodes and edges were returned. Using a subset so as not to overload your browser.<br>",
					["load " + ADD_TRIPLES_MAX, 
					 "choose predicates to load", 
					 "cancel"],
					[function() { loadTriples(triples); },
					 function() { showPredicateChoiceDialog(triples); },
					function() { }]
				);
			}
		} else {
			ModalIidx.alert("NodeGroup Exec Service Failure", "<b>Error:</b> Results returned from service are not N_TRIPLES");
			return;
		}

		network.body.data.nodes.update(Object.values(nodeDict));
		network.body.data.edges.update(edgeList);
		VisJsHelper.networkBusy(canvasDiv, false);
    };
	
		
    // optLabelIdFlag - True: put URI instead of Type on node label
    // optSkipSAClassFlag - skip triples like "Dog #type owl#class" on main SPARQLgraph display
    //                      these triples can slip in when expanding a node.
    VisJsHelper.addTripleToDicts = function(network, triple, nodeDict, edgeList, optLabelIdFlag, optSkipSAClassFlag, optNodeLabelProp) {
		
		var propIsId = function(p) {
			return optNodeLabelProp && p == optNodeLabelProp;
		}
		
		// add a key value pair to the title
		// lines separated by "<br>"
		// values separated by ","
		// lines are sorted
		var addKeyVal = function(oldStr, newKey, newVal) {
			var lines;
			if (! oldStr) {
				// create from empty oldTitle
				lines = [newKey + ": " + newVal];
				
			} else if (oldStr.match("(^"+newKey+"|br>"+newKey+")")) {
				// add comma-separated newVal to existing line
				lines = oldStr.split("<br>")
				for (var i in lines) {
					if (lines[i].startsWith(newKey + ":")) {
						lines[i] += ", " + newVal;
						
						// split and re-sort the comma-separated values
						keyi = lines[i].split(": ")[0];
						valListi = lines[i].split(": ").slice(1).join(": ").split(', ');
						valListi = [...new Set(valListi)]
						lines[i]=keyi + ": " + valListi.sort().join(", ")
					}
				}
				
			} else {
				// add a new line for this newKey
				lines = oldStr.split("<br>")
				lines.push(newKey + ": " + newVal);
			}
			
			return lines.sort().join("<br>");
		}
		
		var getVal = function(theStr, key) {
			
			for (var line of theStr.split("<br>")) {
				if (line.startsWith(key + ":")) {
					return line.split(": ").slice(1).join(": ");
				}
			}
			return ""
		}
		
		// if node is not in nodeDict then pull it from network into nodeDict
		var ensureNodeInDict = function(id) {
			if (nodeDict[id] === undefined) {
				if (id in network.body.nodes) {
					// pull the nodeDict entry out of the network if possible
					var n = network.body.nodes[id].options;
					nodeDict[id] = { id: n.id };
					nodeDict[id].group = n.group;
					nodeDict[id][MOUSEOVER] = n[MOUSEOVER];
					nodeDict[id][LABEL] = n[LABEL];
				} else {
					// create a blank-ish node if there's nothing in the network either
					nodeDict[id] = { id: id , group: UNTYPED };
					nodeDict[id][MOUSEOVER] = addKeyVal("", URI, formatUri(id));
					nodeDict[id][LABEL] = optLabelIdFlag ? formatUri(id) : "";
				}
			}
		}
		
		var s = triple[0];
		var p = triple[1];
		var o = triple[2];
		var oType = null;
		
		// shorten uri for viewers if optLabelIdFlag
		var formatUri = function(uri) {
			return (optLabelIdFlag ? uri.split("/").slice(-1)[0] : uri);
		}
		
		var formatProp = function(p) {
			return p.split(/\/|#/).slice(-1)[0]
		}
		
		// clean up triples removing <> and quotes, etc.
		if (s.startsWith("<")) 
			s = s.slice(1,-1)  
		if (p.startsWith("<"))    
			p = p.slice(1,-1)     
		
		
		// if o is quoted, make it double-quotes
		if (o.startsWith("'")) {
			o[o.indexOf("'")] = "\"";
			o[o.lastIndexOf("'")] = "\"";
		}
		
		// process o into o and oType
		// oType is "uri", typed by suffix, or BLANK_NODE
		if (o.startsWith("<")) {
			o = o.slice(1,-1);
			oType = "uri";         // untyped URI
			
			if (optSkipSAClassFlag) {
				if (this.isTypePredicate(p) && o.toLowerCase().endsWith("owl#class")) {
					return;
				}
			}
		} else if (o.startsWith("\"") ) {
			prev = o;
			o = prev.slice(1, prev.lastIndexOf("\""));   // strips off "" and possibly "@english or ^^my#type
			oType = prev.slice(prev.lastIndexOf("\"") + 1, )
			if (oType.startsWith("^^"))
				oType = oType.slice(2,)
			else {
				// nothing or @language both mean string
				oType = "string"
			}
		} else if (o.match(VisJsHelper.BLANK_NODE_REGEX)) {
			oType = VisJsHelper.BLANK_NODE;
		}
		
		var UNTYPED = "untyped";
		var MOUSEOVER = "title";
		var LABEL = "label";
		var URI = "Uri";
		var TYPE = "Type";
		
		ensureNodeInDict(s);
			
		if (this.isTypePredicate(p)) {
			
			// node type
			longType = o;
			shortType = o.indexOf("#")>-1 ? o.split("#").slice(-1,)[0] : o;
			
			nodeDict[s][MOUSEOVER] = addKeyVal(nodeDict[s][MOUSEOVER], TYPE, shortType);
			nodeDict[s][MOUSEOVER] = addKeyVal(nodeDict[s][MOUSEOVER], URI,  formatUri(s));
			
			if (! nodeDict[s][LABEL]) {
				// fill in LABEL if it is not already overridden by a property
				nodeDict[s][LABEL] = getVal(nodeDict[s][MOUSEOVER], optLabelIdFlag?URI:TYPE);
			}
        	
        	if (nodeDict[s]["group"] === undefined || nodeDict[s]["group"] === VisJsHelper.BLANK_NODE || nodeDict[s]["group"] === UNTYPED ) {
				// group was empty
        		nodeDict[s]["group"] = longType;
        	} else {
				// split, sort, uniqifty long types
				types = nodeDict[s]["group"].split(",");
				types.push(longType);
				types = [...new Set(types)]
				types.sort()
				nodeDict[s]["group"] = types.join(",");
			}
		} else {
			// edge
			var predName = (p.indexOf("#") > -1) ? p.split("#")[1] : p;
			
			if (oType == "uri") {
				// edge to uri
				
				ensureNodeInDict(o);
				
				edgeList.push({
                            id: s + "-" + predName  +"-" + o,  // prevent duplicate edges
                            from: s,
                            to: o,
                            label: predName,
                            arrows: 'to',
                            color: {inherit: false},
                            //group: key      // removed this not sure what it is supposed to be for edges
                        });
                        
			} else if (propIsId(p)) {
				// already made sure subject node exists
				
				// add prop to MOUSEOVER
				nodeDict[s][MOUSEOVER] = addKeyVal(nodeDict[s][MOUSEOVER], formatProp(p), o);
				nodeDict[s][LABEL] = getVal(nodeDict[s][MOUSEOVER], formatProp(p));
				
			} else {
                // edge to a data data or blank
				
                var g = (oType == VisJsHelper.BLANK_NODE) ? VisJsHelper.BLANK_NODE : VisJsHelper.DATA_NODE;
                // allow typing blank nodes if optLabelIdFlag
                g = (optLabelIdFlag && nodeDict[o] && nodeDict[o]["group"]) ? nodeDict[o]["group"] : g;

                nodeDict[o] = {
                    id : o,
                    label : (oType == VisJsHelper.BLANK_NODE) ? "_:blank" : o,
                    title : (oType == VisJsHelper.BLANK_NODE) ? o : oType,
                    group: g,
                };

                // add the edge
                //console.log(j["@id"]+"-"+predName+"-"+val);
                edgeList.push({
                    id: s + "-" + predName + "-" + o,   // prevent duplicate edges
                    from: s,
                    to: o,
                    label: predName,
                    arrows: 'to',
                    color: {inherit: false},
                    group: p
                });
            }
		}
    };

	/*
     * for network n created with VisJsHelper,
     * get the node ids of an edge: [from_node_id, to_node_id]
     */
    VisJsHelper.getNetworkEdgeFromTo = function(n, edgeId) {
		var edge =  n.body.data.edges._data[edgeId];
		return [edge.from, edge.to]
    };
    
    /*
     * for network n created with VisJsHelper,
     * get the object property URI of an edge.
     */
    VisJsHelper.getNetworkEdgeUri = function(n, edgeId) {
		// .group holds the Uri
		return n.body.data.edges._data[edgeId].group;
    };
    /*
     * for network n created with VisJsHelper,
     * get class property of a node.
     * Returns null for BLANK nodes or DATA nodes
     */
    VisJsHelper.getNetworkNodeUri = function(n, nodeId) {
		// .group holds the Uri
		var g = n.body.data.nodes._data[nodeId].group;
		return (g == VisJsHelper.BLANK_NODE || g == VisJsHelper.DATA_NODE) ? null : g;
    };

    /**
	 * Determines if a string is a type predicate.  May include angled brackets or not. 
	 * (e.g. <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)
	 */
    VisJsHelper.isTypePredicate = function(p) {
        return p.toLowerCase().endsWith("#type") || p.toLowerCase().endsWith("#type>");
    };
   
	/**
	 * Removes prefix from URI (e.g. http://item#description => description) if present
	 */
	VisJsHelper.stripPrefix = function(uri) {
		return (uri.indexOf("#") > -1) ? uri.split("#")[1] : uri;
	}

    /*
     * create a unique id with base name
     */
    VisJsHelper.test = function(base) {

    };
    return VisJsHelper;
});
