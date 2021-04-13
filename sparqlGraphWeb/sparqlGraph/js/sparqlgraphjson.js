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

/*
 *
 * This is just for lumping a sparqlConnection with a SemanticNodeGroup so they can be stored and retrieved together
 *
 */
define([	// properly require.config'ed
            'sparqlgraph/js/plotspecshandler',

			// shimmed
        	'sparqlgraph/js/belmont',

		],

	function(PlotSpecsHandler) {

        /*
         * Each param may be missing or null
         */
		var SparqlGraphJson = function(conn, nodegroup, importSpec, deflateFlag, plotSpecsHandler) {
			var deflateFlag = (typeof deflateFlag == "undefined") ? true : deflateFlag;

			this.jObj = {
                    version: 3,
					sparqlConn: null,
					sNodeGroup: null,
					importSpec: null,
                    plotSpecs: null
			};

			if (typeof conn      != "undefined" && conn != null) {
				this.setSparqlConn(conn);
			}

			if (typeof nodegroup != "undefined" && nodegroup != null) {
				if (deflateFlag) {
					var undeflatablePropItems = (typeof importSpec != "undefined" && importSpec != null) ? importSpec.getUndeflatablePropItems() : [];
					this.jObj.sNodeGroup = nodegroup.toJson(true, undeflatablePropItems);
				} else {
					this.jObj.sNodeGroup = nodegroup.toJson(false, []);
				}
			}

			if (typeof importSpec != "undefined" && importSpec != null) {
				this.jObj.importSpec = importSpec.toJson();
			}

            if (typeof plotSpecsHandler != "undefined" && plotSpecsHandler != null) {
				this.jObj.plotSpecs = plotSpecsHandler.toJson();
			}

		};

		SparqlGraphJson.prototype = {

            deepCopy : function() {
                var j = JSON.parse(JSON.stringify(this.jObj));
                var ret = new SparqlGraphJson(undefined, undefined, undefined, false, undefined);
                ret.jObj = j;
                return ret;
            },

            setExtra : function(name, json) {
                this.jObj[name] = json;
            },

            /*
             * return extra field   or null
             */
            getExtra : function(name) {
                if (this.jObj.hasOwnProperty(name)) {
                    return this.jObj[name];
                } else {
                    return null;
                }
            },

            /*
             * get version integer
             */
            getVersion : function() {
                if (this.jObj.hasOwnProperty("version")) {
                    return this.jObj.version;
                } else {
                    return 0;
                }
            },

            /*
             * return the connection    or null if this is an old SparqlForm file
             */
			getSparqlConn : function() {
                if (this.jObj.hasOwnProperty("sparqlConn")) {
                    var ret = new SparqlConnection();
                    ret.fromJson(this.jObj.sparqlConn);
                    return ret;
                } else {
                    return null;
                }
			},

            /*
             * return the nodegroup    never null
             */
			getNodeGroup : function(ng) {
				// different from Java due to canvas stuff: takes ng param.

				ng.clear();
				var json = this.getSNodeGroupJson();
				if (json == null) {
					return null;
				} else {
					ng.addJson(json);
					ng.setSparqlConnection(this.getSparqlConn());
				}
			},

            /*
             * returns a (possibly empty) PlotSpecsHandler
             */
            getPlotSpecsHandler : function() {
                if (this.jObj.hasOwnProperty("plotSpecs") && this.jObj.plotSpecs) {
                    return new PlotSpecsHandler(this.jObj.plotSpecs);
                } else {
                    return new PlotSpecsHandler();
                }
            },

			getSNodeGroupJson : function() {
                if (this.jObj.hasOwnProperty("sNodeGroup")) {
                    return this.jObj.sNodeGroup;
                } else if (this.jObj.hasOwnProperty("nodeGroup")) {
                    return this.jObj.nodeGroup;
                } else {
                    throw new Error("JSON has no nodegroup");
                }

			},

            // horrible depricated name
			getMappingTabJson : function() {
				return this.getImportSpecJson();
			},

            getImportSpecJson : function() {
				if (this.jObj.hasOwnProperty("importSpec")) {
					return this.jObj.importSpec;
				} else {
					return null;
				}
			},

			setSparqlConn : function(conn) {
				this.jObj.sparqlConn = conn.toJson();
			},

            setPlotSpecsHandler : function(handler) {
                this.jObj.plotSpecs = handler.toJson();
            },

			stringify : function () {
				return JSON.stringify(this.jObj, null, '\t');
			},

			parse : function (jsonString) {
				this.jObj = JSON.parse(jsonString);
			},

			toJson : function () {
				return this.jObj;
			},

			fromJson : function (json) {
				this.jObj = json;
			}

		};
		return SparqlGraphJson;
	}
);
