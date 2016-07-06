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
 *  A complicated HTML modal dialog for loading an ontology.
 *
 *
 * In your HTML, you need:
 * 		1) the stylesheet
 * 			<link rel="stylesheet" type="text/css" href="../css/modaldialog.css" />
 * 
 * 		2) an empty div named "modaldialog"
 * 			<div id="modaldialog"></div>
 *
 * NEEDS THESE:
        <script type="text/javascript" src="../cookiemanager.js"></script>
        <script type="text/javascript" src="../sparqlconnection.js"></script>
 */



var ModalLoadDialog = function(document, varName) {
	// strangely, "varName" is the name of the variable holding this dialog in your HTML
	// "divId" is the id of a <div> somewhere (anywhere) in your HTML that is otherwise unused
	
	this.document = document;
	this.div = document.getElementById("modaldialog");
	this.cookieManager = new CookieManager(document);
	
	this.varName = varName;
	this.callback = null;
	this.numFields = 0;
	
	// HTML form with %VAR substituted for variable names and all ids starting with "md"
	this.html = ' \
	<div id="modaldialog_div" style="width:90ch;">\
	<center>\
	<form name="loadDialogForm" action="javascript:%VAR.callbackSubmit();">\
	<table border="1">\
		<tr> \
			<td> <!-- TABLE: Left  -->\
		\
	    		<form class="form-horizontal"> </form> <!-- I have no idea why it all crashes and burns without this -->\
				<form class="form-horizontal">\
				<fieldset> \
					<legend>Profile</legend>\
					<div class="control-group"><label class="control-label">Name</label>     <div class="controls"><input type="text" class="input-xlarge" id="mdName"></div></div>\
					<div class="control-group"><label class="control-label">Type:</label><div class="controls">\
						<label class=radio><input type="radio" name="mdServer" id="mdTypeFuseki" onClick="%VAR.callbackServerType();" value="F">Fuseki</label>\
						<label class=radio><input type="radio" name="mdServer" id="mdTypeVirtuoso" onClick="%VAR.callbackServerType();" value="V" checked>Virtuoso</label>\
						<label class=radio><input type="radio" name="mdServer" id="mdTypeGE" onClick="%VAR.callbackServerType();" value="G">QueryServer</label>\
					</div></div> \
					<div class="control-group"><label class="control-label">Domain:</label>          <div class="controls"><input type="text" class="input-xlarge"  id="mdDomain"></div></div>\
\
					<legend>Data Endpoint</legend>\
					<div class="control-group"><label class="control-label">Server URL:</label>      <div class="controls"><input type="text" class="input-xlarge"  id="mdDataServerURL"></div></div>\
					<div class="control-group"><label class="control-label">KS URL:</label>   <div class="controls"><input type="text" class="input-xlarge disabled" disabled id="mdDataKsURL"></div></div>\
					<div class="control-group"><label class="control-label" id="mdSDS0">Dataset:</label><div class="controls"><input type="text" class="input-xlarge"  id="mdDataSource"></div></div>\
\
					<legend>Ontology Endpoint</legend>\
					<div class="control-group"><label class="control-label">Server URL:</label>      <div class="controls"><input type="text" class="input-xlarge"  id="mdOntologyServerURL"></div></div>\
					<div class="control-group"><label class="control-label">KS URL:</label>   <div class="controls"><input type="text" class="input-xlarge disabled" disabled  id="mdOntologyKsURL"></div></div>\
					<div class="control-group"><label class="control-label" id="mdSDS1">Dataset:</label><div class="controls"><input type="text" class="input-xlarge"  id="mdOntologySource"></div></div>\
\
				</fieldset>\
				</form>\
			</td>\
			<td valign="top"> <!-- TABLE: Right -->\
				<legend>Server Profiles</legend>\
				<select id="mdSelectProfiles" size=20 style="min-width:100%;" onchange="%VAR.callbackSelectionChange();">\
				</select>\
				<div class="form-actions" align="right"> \
					<button type="button" class="btn" id="mdProfileImport" onClick="%VAR.importProfiles();">Import</button>\
					<button type="button" class="btn" id="mdProfileExport" onClick="%VAR.exportProfiles();">Export</button>\
		        	<br><br>\
					<button type="button" class="btn" id="mdProfileAdd"    onClick="%VAR.callbackSave();">Save</button>\
					<button type="button" class="btn" id="mdProfileDelete" onClick="%VAR.callbackDelete();">Delete</button>\
				</div>\
			</td>\
		</table>\
		<div class="form-actions" align="right"> \
			<input type="button" class="btn-danger" value="Cancel" onClick="%VAR.callbackCancel();"></input> \
		    <input type="submit" class="btn-primary" value="Submit"></input>\
		</div>\
	</form>\
	</center>\
	</div>\
	'.replace(/%VAR/g, varName);
	
	this.hide();
};

ModalLoadDialog.COOKIE_NAME = "mdProfile";
ModalLoadDialog.COOKIE_NAME_INDEX = "mdIndex";

ModalLoadDialog.prototype = {

	getLastConnectionInvisibly : function() {
		// while remaining hidden, return last loaded conn or null
		
		// get the index
		var index = this.cookieManager.getCookie(ModalLoadDialog.COOKIE_NAME_INDEX);
		if (index == null) {
			return null;
		}
		
		// get cookie
		var cookieStr = this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, index);
		return new SparqlConnection(cookieStr);
	},
	
	loadDialog : function (curConn, callback) {
		// load dialog   
		// callback(sparqlconnection)
		this.callback = callback;
		this.div.innerHTML = this.html;
		this.readProfiles(curConn);
		this.show();
	},
	
	//*** Callbacks ***//
	callbackServerType : function () {
		// for this part (at the moment), Fuseki and Virtuoso look the same
		if (this.document.getElementById("mdTypeFuseki").checked || this.document.getElementById("mdTypeVirtuoso").checked) {
			this.document.getElementById("mdDataKsURL").className = "input-xlarge disabled";
			this.document.getElementById("mdOntologyKsURL").className = "input-xlarge disabled";
			this.document.getElementById("mdDataKsURL").disabled = true;
			this.document.getElementById("mdOntologyKsURL").disabled = true;
			
			this.document.getElementById("mdSDS0").innerHTML="Dataset";
			this.document.getElementById("mdSDS0").innerHTML="Dataset";

		// GE QueryServer
		} else {
			this.document.getElementById("mdDataKsURL").className = "input-xlarge";
			this.document.getElementById("mdOntologyKsURL").className = "input-xlarge";
			this.document.getElementById("mdDataKsURL").disabled = false;
			this.document.getElementById("mdOntologyKsURL").disabled = false;
			
			this.document.getElementById("mdSDS0").innerHTML="Source";
			this.document.getElementById("mdSDS0").innerHTML="Source";
		}
	},
	
	callbackSave : function () {
		// Save the profile on the screen
		//     give it a name if there is none
		//     overwrite selected profile if names match, otherwise add it to the end
		
		// make sure profile has a name
		if (this.document.getElementById("mdName").value == "") {
			this.document.getElementById("mdName").value = "unnamed_profile";
		}

		var profile = this.createProfileFromScreen();
		
		var select = this.document.getElementById("mdSelectProfiles");
		var name = this.document.getElementById("mdName").value;
		var itemIdx = this.findProfileByName(name);
		
		// change the selected profile if the names match
		if (itemIdx > -1) {
			select.options[itemIdx].value = profile.toString();
		// otherwise create a new profile
		} else {
			this.appendProfile(profile);
			this.setSelectedIndex(select.length-1);
			this.sortProfiles();
		}
		this.displaySelectedProfile();
	},
	
	callbackDelete : function () {
		var i = this.getSelectedIndex();
		var select = this.document.getElementById("mdSelectProfiles");
		// do nothing if nothing is selected
		if (i < 0) {
			return;
		}
		select.options[i].selected = false;
		select.remove(i);
		
		this.displaySelectedProfile();
	},
	
	callbackSubmit : function () {
		// save all the saved profiles and return whatever is showing
		this.hide();
		var profile = this.createProfileFromScreen();
		this.writeProfiles();
		this.callback(profile);
	},
	
	callbackCancel : function () {
		this.hide();
	},
	
	callbackSelectionChange : function () {
		// selection changes, including to -1
		this.displaySelectedProfile();
	},
	
	connectionIsKnown : function (conn, selectFlag) {
		// search through connections, ignoring the name and return the name if found
		
		// set up dialog without showing it
		this.div.innerHTML = this.html;
		// load the profiles without showing them
		this.readProfiles();
		var select = this.document.getElementById("mdSelectProfiles");

		for (var i=0; i < select.length; i++) {
			var other = new SparqlConnection(select.options[i].value);
			if (conn.equals(other, true)) {   // true means ignore the name
				if (selectFlag) {
					this.setSelectedIndex(i);
				}
				return other.name;
			} 
		}
		return false;
	},
	
	addConnection : function(conn) {
		// Appends and writes a profile
		// So you must first call something that reads the profiles
		// and also check that it doesn't already exist.
		this.appendProfile(conn);
		
		var select = this.document.getElementById("mdSelectProfiles");
		this.setSelectedIndex(select.length-1);
		this.sortProfiles();
		this.writeProfiles();
	},
	
	//***  Meant to be private ***//
	hide : function () {
		// private
		this.div.style.visibility = "hidden" ;
	},
	
	show : function () {
		// private
		this.div.style.visibility = "visible";
	},
	
	getSelectedIndex : function () {
		return this.document.getElementById("mdSelectProfiles").selectedIndex;
	},
	
	setSelectedIndex : function (i) {
		this.document.getElementById("mdSelectProfiles").selectedIndex = i;
	},

	findProfileByName : function (label) {
		var select = this.document.getElementById("mdSelectProfiles");

		for (var i=0; i < select.length; i++) {
			if (select.options[i].label == label) {
				return i;
			} 
		}
		return -1;
	},
	
	displaySelectedProfile : function() {
		index = this.getSelectedIndex();
		var select = this.document.getElementById("mdSelectProfiles");
		var profileStr = (select.selectedIndex > -1) ? select.options[select.selectedIndex].value : null;
		
		if (profileStr == null) {
			document.getElementById("mdName").value = "";
			
			document.getElementById("mdTypeFuseki").checked = false;
			document.getElementById("mdTypeVirtuoso").checked = false;
			document.getElementById("mdTypeGE").checked = true;
			
			document.getElementById("mdDataServerURL").value = "";
			document.getElementById("mdDataKsURL").value = "";
			document.getElementById("mdDataSource").value = "";
			
			document.getElementById("mdOntologyServerURL").value = "";
			document.getElementById("mdOntologyKsURL").value = "";
			document.getElementById("mdOntologySource").value = "";

			document.getElementById("mdDomain").value = "";
			
		} else {
			var profile = new SparqlConnection();
			profile.fromString(profileStr);
			document.getElementById("mdName").value = profile.name;
			switch (profile.serverType) {
				case SparqlConnection.FUSEKI_SERVER:
					this.document.getElementById("mdTypeFuseki").checked = true;
					break;
				case SparqlConnection.VIRTUOSO_SERVER:
					this.document.getElementById("mdTypeVirtuoso").checked = true;
					break;
				case SparqlConnection.QUERY_SERVER:
					this.document.getElementById("mdTypeGE").checked = true;
					break;
				default:
					alert("Warning unknown server type in profile:" + profile.serverType)
			}
			
			this.document.getElementById("mdDataServerURL").value = profile.dataServerUrl;
			this.document.getElementById("mdDataKsURL").value = profile.dataKsServerURL;
			this.document.getElementById("mdDataSource").value = profile.dataSourceDataset;
			
			this.document.getElementById("mdOntologyServerURL").value = profile.ontologyServerUrl;
			this.document.getElementById("mdOntologyKsURL").value = profile.ontologyKsServerURL;
			this.document.getElementById("mdOntologySource").value = profile.ontologySourceDataset;

			this.document.getElementById("mdDomain").value = profile.domain;
		}
		
		this.callbackServerType(); // enable/disable fields based on 
	},
	
	createProfileFromScreen : function() {
		// create a profile from the ones on the screen
		var profile = new SparqlConnection();
		profile.name = this.document.getElementById("mdName").value.trim();
		
		if (this.document.getElementById("mdTypeFuseki").checked) {
			profile.serverType = SparqlConnection.FUSEKI_SERVER;
		} else if (this.document.getElementById("mdTypeVirtuoso").checked) {
			profile.serverType = SparqlConnection.VIRTUOSO_SERVER;
		} else {
			profile.serverType = SparqlConnection.QUERY_SERVER;
		}
		
		profile.dataServerUrl = this.document.getElementById("mdDataServerURL").value.trim();
		profile.dataKsServerURL = this.document.getElementById("mdDataKsURL").value.trim();
		profile.dataSourceDataset = this.document.getElementById("mdDataSource").value.trim();
		
		profile.ontologyServerUrl = this.document.getElementById("mdOntologyServerURL").value.trim();
		profile.ontologyKsServerURL = this.document.getElementById("mdOntologyKsURL").value.trim();
		profile.ontologySourceDataset = this.document.getElementById("mdOntologySource").value.trim();
		
		// If ontology stuff is empty, set it to same as data stuff
		if (profile.ontologyServerUrl == "" && profile.ontologyKsServerURL == "" && profile.ontologySourceDataset == "") {
			profile.ontologyServerUrl = profile.dataServerUrl;
			profile.ontologyKsServerURL = profile.dataKsServerURL;
			profile.ontologySourceDataset = profile.dataSourceDataset;
		}

		profile.domain = this.document.getElementById("mdDomain").value;
		// Adds the http:// on front of domain if needed
		if (profile.domain.indexOf("http") != 0) {
			profile.domain = "http://" + profile.domain;
		}
		
		profile.build();
		return profile;
	},
	
	appendProfile : function (profile) {

		var opt = new Option(profile.name, profile.toString());
		this.document.getElementById("mdSelectProfiles").appendChild(opt);
	},
	
	
	readProfiles : function (curConn) {

		var i = 0;
		var cookieStr = this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);
		var profile;
		var curIndex = -1;
		var select = this.document.getElementById("mdSelectProfiles");
		
		// load all the connections from cookies
		do {
			profile = new SparqlConnection(cookieStr);
			// does this cookied profile match curConn.  
			// 'True' indicates we want to ignore the curConn profile name.
			if (curConn && curConn.equals(profile, true)) {
				curIndex = i;
			}
			this.appendProfile(profile);
			
			i += 1;
			cookieStr = this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);
		} while (cookieStr != null);
		this.sortProfiles();
		
		// if there is a connection loaded, make sure it is in the cookies
		if (curConn) {
			if (curIndex > -1) {
				// select the current conn in the list of loaded connections
				select.selectedIndex = curIndex;
			} else {
				// add the current conn to the list of loaded connections and select it
				// PEC TODO: This could add a new connection with a repeat name.  No real harm done, I think.
				// PEC TODO: The new connection will be saved if the user hits "Save" or "Submit", but not if they hit "Cancel".  Not sure what is desired behavior.
				this.appendProfile(curConn);
				select.selectedIndex = i;
			}
		// if no connection loaded, load the one from last session
		} else {
			// select the conn indicated in COOKIE_NAME_INDEX
			var index = this.cookieManager.getCookie(ModalLoadDialog.COOKIE_NAME_INDEX);
			if (index != null) {
				select.selectedIndex = index;
			}
		}
		this.sortProfiles();
		this.callbackSelectionChange();
	},
	
	sortProfiles : function () {
		var select = this.document.getElementById("mdSelectProfiles");
		var selectedValue = null; 
		if (select.selectedIndex > -1) {
			selectedValue = select[select.selectedIndex].value;
		}
		var newSelectedIndex=-1;
		var tmpAry = new Array();
	    for (var i=0;i<select.options.length;i++) {
	        tmpAry[i] = new Array();
	        tmpAry[i][0] = select.options[i].text;
	        tmpAry[i][1] = select.options[i].value;
	    }
	    tmpAry.sort();
	    while (select.options.length > 0) {
	        select.options[0] = null;
	    }
	    for (var i=0;i<tmpAry.length;i++) {
	        var op = new Option(tmpAry[i][0], tmpAry[i][1]);
	        select.options[i] = op;
	        if (tmpAry[i][1] == selectedValue)
	        	newSelectedIndex= i;
	    }
	    select.selectedIndex = newSelectedIndex;
	    return;
	},
	
	writeProfiles : function () {
		// save profiles in cookies
		
		var select = this.document.getElementById("mdSelectProfiles");

		// delete all indexed cookies, I hope
		// this will help the "too many cookies" error to come up at a smarter time
		for (var i=0; i < select.length + 24; i++) {
			if (this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, i) != null) {
				this.cookieManager.delIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);
			}
		}
		
		// loop through profiles and save to cookieManager
		for (var i=0; i < select.length; i++) {
			this.cookieManager.setIndexedCookie(ModalLoadDialog.COOKIE_NAME, i, select.options[i].value.toString());
		}
		
		// save the index of the selected profile
		this.cookieManager.setCookie(ModalLoadDialog.COOKIE_NAME_INDEX, select.selectedIndex);
	}, 
	
	exportProfiles : function () {
		var result = "";
		var select = this.document.getElementById("mdSelectProfiles");

		// loop through profiles and save to cookieManager
		for (var i=0; i < select.length; i++) {
			result += select.options[i].value.toString() + "\n";
		}
		console.log(result);
		this.downloadFile(result, "profiles.txt");
	},
	
	importProfiles : function () {
		var text = prompt("Paste profiles here:", " ");
		var lines = text.split(/[{}\s]+/);
		var profile = null;
		
		for (var i=0; i < lines.length; i++) {
			if (lines[i].length < 1) continue;
			try {
				profile = new SparqlConnection('{' + lines[i] + '}');
				this.appendProfile(profile);
			} catch (err) {
				alert("Could not import this line:\n" + lines[i]);
			}
		}
	},
	
	downloadFile : function (data, filename) {
    	// build an anchor and click on it
		$('<a>invisible</a>')
			.attr('id','downloadFile')
			.attr('href','data:text/csv;charset=utf8,' + encodeURIComponent(data))
			.attr('download', filename)
			.appendTo('body');
		$('#downloadFile').ready(function() {
			$('#downloadFile').get(0).click();
		});
		
		// remove the evidence
		var parent = this.document.getElementsByTagName("body")[0];
		var child = this.document.getElementById("downloadFile");
		parent.removeChild(child);
    },
};