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
 * Double connection in case Ontology is in a different place than the data
 * 
 * NEEDS:
 	<script type="text/javascript" src="../jquery/jquery.jsonp-1.0.4.min.js"></script>
 	<script type="text/javascript" src="../js/fusekiserverinterface.js"></script>
 	
	<script type="text/javascript" src='../js/queryserverinterface.js'></script>	
	
 * This has evolved into something embarrassing.  
 * It seems to duplicate everything in its two interfaces.
 * You also need to call build() after mucking with any of its name strings (except domain).
 */



var SparqlConnection = function(text) {
	if (text) {
    	this.fromString(text);
    } else {
    	this.name = "";
		this.serverType = "";
		
		this.dataServerUrl = "";
		this.dataKsServerURL = "";   // fuseki will not have this
		this.dataSourceDataset = "";
		
		this.ontologyServerUrl = "";
		this.ontologyKsServerURL = "";   // fuseki will not have this
		this.ontologySourceDataset = "";
		
		this.domain = "";
		this.dataInterface = null;
		this.ontologyInterface = null;
    }
};

SparqlConnection.NONE_SERVER = "";
SparqlConnection.QUERY_SERVER = "kdl";
SparqlConnection.FUSEKI_SERVER = "fuseki";
SparqlConnection.VIRTUOSO_SERVER = "virtuoso";
SparqlConnection.DELIM = '>';

SparqlConnection.prototype = {
	
	toJson : function () {
		var jObj = {
			name: this.name,
			type:                 this.serverType,
				
			dsURL:        this.dataServerUrl,
			dsKsURL:      this.dataKsServerURL,   // fuseki will not have this
			dsDataset:    this.dataSourceDataset,
				
			domain:this.domain,	
		};
		
		if (this.ontologyServerUrl != this.dataServerUrl)         { jObj.onURL=  this.ontologyServerUrl; }
		if (this.ontologyKsServerURL != this.dataKsServerURL)     { jObj.onKsURL = this.ontologyKsServerURL; }
		if (this.ontologySourceDataset != this.dataSourceDataset) { jObj.onDataset = this.ontologySourceDataset; }
		return jObj;
	},
	
	fromJson : function (jObj) {
		
		// old verbose mode had "serverType"
		if (jObj.hasOwnProperty("serverType")) {
			this.name = jObj.name; 
			this.serverType = jObj.serverType; 
				
			this.dataServerUrl = jObj.dataServerUrl;
			this.dataKsServerURL = jObj.dataKsServerURL;
			this.dataSourceDataset = jObj.dataSourceDataset;
				
			this.ontologyServerUrl = jObj.ontologyServerUrl;
			this.ontologyKsServerURL = jObj.ontologyKsServerURL;
			this.ontologySourceDataset = jObj.ontologySourceDataset;
			 
			this.dataInterface = this.createDataInterface();
			this.ontologyInterface = this.createOntologyInterface();
			this.domain = jObj.domain;
			
		// newer compact mode has "st"
		} else {
			this.name = jObj.name; 
			this.serverType = jObj.type; 
				
			this.dataServerUrl = jObj.dsURL;
			this.dataKsServerURL = jObj.dsKsURL;
			this.dataSourceDataset = jObj.dsDataset;
				
			this.ontologyServerUrl =     jObj.hasOwnProperty("onURL") ?     jObj.onURL : jObj.dsURL;
			this.ontologyKsServerURL =   jObj.hasOwnProperty("onKsURL") ?   jObj.onKsURL : jObj.dsKsURL;
			this.ontologySourceDataset = jObj.hasOwnProperty("onDataset") ? jObj.onDataset : jObj.dsDataset;
			 
			this.dataInterface = this.createDataInterface();
			this.ontologyInterface = this.createOntologyInterface();
			
			this.domain = jObj.domain;
		}
	},
	
	fromString : function (text) {
		
		try {
			// try JSON first
			jObj = JSON.parse(text);
			this.fromJson(jObj);
			
		} catch (e) {
			// JSON failed, try backwards-compatibility mode
			alert("Reading backwards-compatible sparql connection cookies.\nYou may get a bunch of these alerts in a row, but click through them.\nYou should never see this again.");
			field = text.split(SparqlConnection.DELIM);
			
			this.name =              field[0];
			this.serverType =        field[1];
			
			this.dataServerUrl =     field[2];
			this.dataKsServerURL =   field[3];
			this.dataSourceDataset = field[4];
			
			this.ontologyServerUrl =     field[5];
			this.ontologyKsServerURL =   field[6];
			this.ontologySourceDataset = field[7];
			
			this.dataInterface = this.createDataInterface();
			this.ontologyInterface = this.createOntologyInterface();
			this.domain =            field[8];
		}
	},
	
	toString : function () {
		return JSON.stringify(this.toJson());
	},
	
	// Deprecated
	toStringBackwardsCompatible : function () {
		var ret = "";
		ret += this.name              + SparqlConnection.DELIM;
		ret += this.serverType        + SparqlConnection.DELIM;

		ret += this.dataServerUrl     + SparqlConnection.DELIM;
		ret += this.dataKsServerURL   + SparqlConnection.DELIM;
		ret += this.dataSourceDataset + SparqlConnection.DELIM;
		
		ret += this.ontologyServerUrl     + SparqlConnection.DELIM;
		ret += this.ontologyKsServerURL   + SparqlConnection.DELIM;
		ret += this.ontologySourceDataset + SparqlConnection.DELIM;
		
		ret += this.domain;
		return ret;
	},
	
	equals : function (other, ignoreName) {
		return ((ignoreName || this.name == other.name) && 
				this.serverType == other.serverType &&
				this.dataInterface.equals(other.dataInterface) && 
				this.ontologyInterface.equals(other.ontologyInterface) && 
				this.domain == other.domain);
	},
	
	build : function () {
		// needs to be called after mucking with members
		this.dataInterface = this.createDataInterface();
		this.ontologyInterface = this.createOntologyInterface();
	},
	
	setup(name, serverType, domain) {
		this.name = name;
		this.serverType = serverType;
		this.domain = domain;
	}, 
	
	setOntologyInterface(url, dataset, optKsURL) {
		
		this.ontologyServerUrl =     url;
		this.ontologyKsServerURL =   optKsURL;
		this.ontologySourceDataset = dataset;
		
		this.ontologyInterface = this.createOntologyInterface();
	},
	
	setDataInterface(url, dataset, optKsURL) {
		
		this.dataServerUrl =     url;
		this.dataKsServerURL =   optKsURL;
		this.dataSourceDataset = dataset;
		
		this.dataInterface = this.createOntologyInterface();
	},
	
	createDataInterface : function () {
		if (this.serverType == SparqlConnection.FUSEKI_SERVER) {
			return new SparqlServerInterface(SparqlServerInterface.FUSEKI_SERVER, this.dataServerUrl, this.dataSourceDataset);
		} else if (this.serverType == SparqlConnection.VIRTUOSO_SERVER) {
			return new SparqlServerInterface(SparqlServerInterface.VIRTUOSO_SERVER, this.dataServerUrl, this.dataSourceDataset);
		} else if (this.serverType == SparqlConnection.QUERY_SERVER) {
			return new QueryServerInterface(this.dataServerUrl, this.dataKsServerURL, this.dataSourceDataset);
		} else {
			return null;
		}
	},
	
	createOntologyInterface : function () {
		if (this.serverType == SparqlConnection.FUSEKI_SERVER) {
			return new SparqlServerInterface(SparqlServerInterface.FUSEKI_SERVER, this.ontologyServerUrl, this.ontologySourceDataset);
		} else if (this.serverType == SparqlConnection.VIRTUOSO_SERVER) {
			return new SparqlServerInterface(SparqlServerInterface.VIRTUOSO_SERVER, this.ontologyServerUrl, this.ontologySourceDataset);
		}else if (this.serverType == SparqlConnection.QUERY_SERVER) {
			return new QueryServerInterface(this.ontologyServerUrl, this.ontologyKsServerURL, this.ontologySourceDataset);
		} else {
			return null;
		}
	}, 
	
	getDataInterface : function () {
		return this.dataInterface;
	}, 
	getOntologyInterface : function () {
		return this.ontologyInterface;
	},
	getDomain : function () {
		return this.domain;
	},
	
};

