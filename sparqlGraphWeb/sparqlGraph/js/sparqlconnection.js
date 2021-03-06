/**
 ** Copyright 2016-17 General Electric Company
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
 * Connection allowing Ontology to be in different place(s) than the data
 * "Interface" for historical reasons is an endpoint plus "graph" or "dataset"
 */

var SparqlConnection = function(jsonText) {

	//--used to support deprecated functions ---
	this.depServerType ="";
	this.depDomain="";
	this.enableOwlImports = false;
	//--old attributes still called by some legacy users ---
	this.serverType = "";

	this.dataServerUrl = "";
	this.dataKsServerURL = "";
	this.dataSourceDataset = "";

	this.ontologyServerUrl = "";
	this.ontologyKsServerURL = "";
	this.ontologySourceDataset = "";

	//-------------------------------------

	// The actual function:
	if (jsonText) {
    	this.fromString(jsonText);
    } else {
    	this.name = "";
    	this.domain = "";

    	this.modelInterfaces = [];
    	this.dataInterfaces = [];
    }
};


SparqlConnection.prototype = {

	toJson : function () {
        // backwards-compat fix.
        if ((typeof this.enableOwlImports == undefined)) {
            this.enableOwlImports = false;
        }

		var jObj = {
			name: this.name,
            // this is no longer editable in SPARQLgraph and defaults to ""
            // it is retained for backwards compatibility
			domain: this.domain,
            enableOwlImports: this.enableOwlImports,
			model: [],
			data: []
		};

		// add model interfaces
		for (var i=0; i < this.modelInterfaces.length; i++) {
			var mi = this.modelInterfaces[i];
			jObj.model.push({
					type: mi.getServerType(),
					url: mi.getServerURL(),
					graph: mi.getGraph()
			});
		}

		// add data interfaces
		for (var i=0; i < this.dataInterfaces.length; i++) {
			var di = this.dataInterfaces[i];
			jObj.data.push({
					type: di.getServerType(),
					url: di.getServerURL(),
					graph: di.getGraph()
			});
		}

		return jObj;
	},

	fromJson : function (jObj) {

		this.name = jObj.name;
		this.domain = jObj.domain;
        // be extra-safe as this is removed except for backwards-compatibility
        if (! this.domain) { this.domain = ""; }

		this.modelInterfaces = [];
    	this.modelDomains = [];
    	this.modelNames = [];

    	this.dataInterfaces = [];
    	this.dataNames = [];

		if (jObj.hasOwnProperty("dsURL")) {

			// backwards compatible read
			console.log("SparqlConnection.fromJson() is reading old-fashioned connection JSON.")

			// If any field doesn't exist, presume it exists in the other connection
			this.addModelInterface(jObj.type,
								  jObj.hasOwnProperty("onURL") ? jObj.onURL : jObj.dsURL,
								  jObj.hasOwnProperty("onDataset") ? jObj.onDataset : jObj.dsDataset
								  );
			this.addDataInterface(jObj.type,
								  jObj.hasOwnProperty("dsURL") ? jObj.dsURL : jObj.onURL,
								  jObj.hasOwnProperty("dsDataset") ? jObj.dsDataset : jObj.onDataset
							      );
		} else {
			// normal read

			// read model interfaces
	    	for (var i=0; i < jObj.model.length; i++) {
	    		var m = jObj.model[i];
                var graph = m.hasOwnProperty("dataset") ? m.dataset : m.graph;
	    		this.addModelInterface(m.type, m.url, graph);
	    	}
	    	// read data interfaces
	    	for (var i=0; i < jObj.data.length; i++) {
	    		var d = jObj.data[i];
                var graph = d.hasOwnProperty("dataset") ? d.dataset : d.graph;
	    		this.addDataInterface(d.type, d.url, graph);
	    	}
		}

        if (jObj.hasOwnProperty("enableOwlImports")) {
            this.enableOwlImports = jObj.enableOwlImports;
        } else {
            this.enableOwlImports = false;
        }
		this.fillDeprecatedFields();
	},

	fromString : function (jsonText) {
		jObj = JSON.parse(jsonText);
		this.fromJson(jObj);
	},

	toString : function () {
		return JSON.stringify(this.toJson());
	},

	equals : function (other, ignoreName) {
		var thisStr = this.toString();
		var otherStr = other.toString();
		if (ignoreName) {
			thisStr = thisStr.replace(this.name, "NAME");
			otherStr = otherStr.replace(other.name, "NAME");
		}
		return (thisStr == otherStr);
	},

	//== New-fashioned way to build a Connection ==
	setName : function (name) {
		this.name = name;
	},

	setDomain : function (domain) {
		this.domain = domain
	},

	addModelInterface : function (sType, url, graph) {
		this.modelInterfaces.push(this.createInterface(sType, url, graph));
	},

	addDataInterface : function (sType, url, graph) {
		this.dataInterfaces.push(this.createInterface(sType, url, graph));
	},

	delModelInterface : function (i) {
		this.modelInterfaces.splice(i, 1);
	},

	delDataInterface : function (i) {
		this.dataInterfaces.splice(i, 1);
	},

    isOwlImportsEnabled : function() {
        return this.enableOwlImports;
    },

    setOwlImportsEnabled : function(enableOwlImports) {
		this.enableOwlImports = enableOwlImports;
	},
	getModelInterfaceCount : function () {
		return this.modelInterfaces.length;
	},
	getModelInterface : function (i) {
		return this.modelInterfaces[i];
	},
	getDomain : function () {
		return this.domain;
	},
	getName : function() {
		return this.name;
	},
	getDataInterfaceCount : function () {
		return this.dataInterfaces.length;
	},
	getDataInterface : function (i) {
		if (typeof i == "undefined") {
			// DEPRECATED: old and new name are the same, but old had no param
			console.log("NOTE: called deprecated SparqlConnection.getDataInterface() with no param");
			return this.dataInterfaces[0];
		} else {
			return this.dataInterfaces[i];
		}

	},

	getDefaultQueryInterface : function () {
		if (this.dataInterfaces.length > 0) {
			return this.dataInterfaces[0];
		} else if (this.modelInterfaces.length > 0) {
			return this.modelInterfaces[0];
		} else {
			throw new Error("This SparqlConnection has no endpoints.");
		}
	},

	getInsertInterface : function () {
		if (this.dataInterfaces.length == 1) {
			return this.dataInterfaces.get[0];
		} else {
			throw new Error("Expecting one data endpoint for INSERT.  Found " + this.dataInterfaces.length);
		}
	},

	// Is number of endpoint serverURLs == 1
	isSingleServerURL : function() {
		var url = "";
		for (var i=0; i < this.modelInterfaces.length; i++) {
			var e =  this.modelInterfaces[i].getServerURL();
			if (url == "") {
				url = e;
			} else if (e != url) {
				return false;
			}
		}

		// add data interfaces
		for (var i=0; i < this.dataInterfaces.length; i++) {
			var e =  this.dataInterfaces[i].getServerURL();
			if (url == "") {
				url = e;
			} else if (e != url) {
				return false;
			}
		}

		// if there are no serverURLs then false
		if (url == "") {
			return false;
		} else {
			return true;
		}
	},

	// get list of graphs for a given serverURL
	getGraphsForServer : function(serverURL) {
		var ret = [];

		for (var i=0; i < this.modelInterfaces.length; i++) {
			var e =  this.modelInterfaces[i];
			if (e.getServerURL() == serverURL &&  ret.indexOf(e.getGraph()) == -1) {
				ret.push(e.getGraph());
			}
		}

		for (var i=0; i < this.dataInterfaces.length; i++) {
			var e =  this.dataInterfaces[i];
			if (e.getServerURL() == serverURL &&  ret.indexOf(e.getGraph()) == -1) {
				ret.push(e.getGraph());
			}
		}

		return ret;
	},

	//---------- private function
	createInterface : function (stype, url, graph) {
        return new SparqlServerInterface(stype, url, graph);
	},

	// DEPRECATED private
	fillDeprecatedFields : function () {
		// make deprecated things "work" using first of each interface
		if (this.modelInterfaces.length < 1 || this.dataInterfaces.length < 1) {
			return;
		}

		this.serverType = this.modelInterfaces[0].getServerType()
		this.ontologyServerUrl = this.modelInterfaces[0].getServerURL();
		this.ontologySourceDataset = this.modelInterfaces[0].getDataset();
		this.dataServerUrl = this.dataInterfaces[0].getServerURL();
		this.dataSourceDataset = this.dataInterfaces[0].getDataset();

	},

	// DEPRECATED: kept for some legacy callers.
	build : function () {
		console.log("ERROR: Using DEPRECATED SparqlConnection functions.");
		console.log("	    Change to setName() addDataInterface() addModelInterface().");

		// creating a single model and single data endpoint presuming that the
		// caller has already set the old-fashioned attributes.
		this.setName(this.name);
		this.addModelInterface(	this.serverType,
								this.ontologyServerUrl,
								this.ontologySourceDataset,
								this.domain,
								""
							);
		this.addDataInterface(	this.serverType,
								this.dataServerUrl,
								this.dataSourceDataset,
								""
							);
	},

	// DEPRECATED
	setup(name, serverType, domain) {
		console.log("NOTE: called deprecated SparqlConnection.setup();");

		this.name = name;
		this.depServerType = serverType;
		this.depDomain = domain;
	},
	// DEPRECATED
	getOntologyInterface : function () {
		console.log("NOTE: called deprecated SparqlConnection.getOntologyInterface()");
		return this.modelInterfaces[0];
	},
	// DEPRECATED
	setOntologyInterface(url, dataset, unused_KsURL) {
		console.log("NOTE: called deprecated SparqlConnection.setOntologyInterface();");

		this.modelInterfaces = [this.createInterface(this.depServerType, url, dataset)];
		this.modelDomains= [this.depDomain];
		this.modelNames=[""];
	},
	// DEPRECATED
	setDataInterface(url, dataset, unused_KsURL) {
		console.log("Note: called deprecated SparqlConnection.setDataInterface();");

		this.dataInterfaces = [this.createInterface(this.depServerType, url, dataset)];
		this.dataNames=[""];
	}
};
