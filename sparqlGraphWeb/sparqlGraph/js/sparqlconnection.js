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
 * Double connection allowing Ontology to be in different place(s) than the data
 */

var SparqlConnection = function(jsonText) {
	
	// used to support deprecated functions
	this.depServerType ="";
	this.depDomain="";
	//-------------------------------------
	
	if (jsonText) {
    	this.fromString(jsonText);
    } else {
    	this.name = "";
    	this.modelInterfaces = [];
    	this.modelDomains = [];
    	this.modelNames = [];
    	
    	this.dataInterfaces = [];
    	this.dataNames = [];
    }
};

SparqlConnection.QUERY_SERVER = "kdl";
SparqlConnection.FUSEKI_SERVER = "fuseki";
SparqlConnection.VIRTUOSO_SERVER = "virtuoso";

SparqlConnection.prototype = {
	
	toJson : function () {
		var jObj = {
			name: this.name,
			model: [],
			data: []
		};
		
		// add model interfaces
		for (var i=0; i < this.modelInterfaces.length; i++) {
			var mi = this.modelInterfaces[i];
			jObj.model.push({
				endpoint: {
					type: mi.getServerType(),
					url: mi.getServerURL(),
					dataset: mi.getDataset()
				},
				domain: this.modelDomains[i],
				name: this.modelNames[i]
			});
		}
		
		// add data interfaces
		for (var i=0; i < this.dataInterfaces.length; i++) {
			var di = this.dataInterfaces[i];
			jObj.model.push({
				endpoint: {
					type: di.getServerType(),
					url: di.getServerURL(),
					dataset: di.getDataset()
				},
				name: this.dataNames[i]
			});
		}
		
		return jObj;
	},
	
	fromJson : function (jObj) {
		
		this.name = jObj.name; 
		
		this.modelInterfaces = [];
    	this.modelDomains = [];
    	this.modelNames = [];

    	this.dataInterfaces = [];
    	this.dataNames = [];
    	
		if (jObj.hasOwnProperty("dsURL")) {
			console.log("SparqlConnection.fromJson() is reading old-fashioned connection JSON.")
		
			// backwards compatible read
			// If any field doesn't exist, presume it exists in the other connection
			this.addModelEndpoint(jObj.type, 
								  jObj.hasOwnProperty("onURL") ? jObj.onURL : jObj.dsURL,
								  jObj.hasOwnProperty("onDataset") ? jObj.onDataset : jObj.dsDataset,
								  jObj.domain,
							      "");
			this.addDataEndpoint( jObj.type, 
								  jObj.hasOwnProperty("dsURL") ? jObj.dsURL : jObj.onURL,
								  jObj.hasOwnProperty("dsDataset") ? jObj.dsDataset : jObj.onDataset,
							      "");
		} else {
			// read model interfaces
	    	for (var i=0; i < jObj.model.length; i++) {
	    		var m = jObj.model[i];
	    		this.addModelEndpoint(m.endpoint.type, m.endpoint.url, m.endpoint.dataset, m.domain, m.name);
	    	}
	    	// read data interfaces
	    	for (var i=0; i < jObj.data.length; i++) {
	    		var d = jObj.data[i];
	    		this.addDataEndpoint(d.endpoint.type, d.endpoint.url, d.endpoint.dataset, d.name);
	    	}
		}
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
	
	addModelEndpoint : function (sType, url, dataset, domain, name) {
		this.modelInterfaces.push(this.createInterface(sType, url, dataset));
		this.modelDomains.push(domain);
		this.modelNames.push(name);
	},
	
	addDataEndpoint : function (sType, url, dataset, name) {
		this.dataInterfaces.push(this.createInterface(sType, url, dataset));
		this.dataNames.push(name);
	},
	
	getModelEndpointCount : function () {
		return this.modelInterfaces.length();
	},
	getModelInterface : function (i) {
		return this.modelInterfaces[i];
	},
	getModelDomain : function (i) {
		return this.modelDomaines[i];
	},
	getModelName : function (i) {
		return this.modelNames[i];
	}, 
	
	getDataEndpointCount : function () {
		return this.dataInterfaces.length();
	},
	getDataInterface : function (i) {
		return this.dataInterfaces[i];
	},
	getDataName : function (i) {
		return this.dataNames[i];
	},

	// DEPRECATED IN A GIANT WAY
	// remove from sparqlform.js, htmlform.js, modalloaddialog.js, uploadtab.js
	build : function () {
		throw new Error("ERROR: If you're calling this you must have changed no-longer-existing attributes like dsDataset");
	},
	
	// DEPRECATED
	setup(name, serverType, domain) {
		console.log("NOTE: called deprecated SparqlConnection.setup();");
		
		this.name = name;
		this.depServerType = serverType;
		this.depDomain = domain;
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
	},
	// DEPRECATED
	getDataInterface : function () {
		console.log("Note: called deprecated SparqlConnection.getDataInterface();");
		return this.dataInterfaces[0];
	}, 
	// DEPRECATED
	getOntologyInterface : function () {
		console.log("Note: called deprecated SparqlConnection.getOntologyInterface();");
		return this.modelInterfaces[0];
	},
	// DEPRECATED
	getDomain : function () {
		console.log("Note: called deprecated SparqlConnection.getDomain();");
		return this.modelDomains[0];
	},
	
	//---------- internal
	createInterface : function (stype, url, dataset) {
		if (stype == SparqlConnection.FUSEKI_SERVER) {
			return new SparqlServerInterface(SparqlServerInterface.FUSEKI_SERVER, url, dataset);
		} else if (stype == SparqlConnection.VIRTUOSO_SERVER) {
			return new SparqlServerInterface(SparqlServerInterface.VIRTUOSO_SERVER, url, dataset);
		} else {
			return null;
		}
	},
	
	
	
};

