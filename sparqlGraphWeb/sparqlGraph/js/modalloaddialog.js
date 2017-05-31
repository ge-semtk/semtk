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



var ModalLoadDialog = function(document, varNameOBSOLETE) {
	
	this.document = document;
	this.div = document.getElementById("modaldialog");
	this.cookieManager = new CookieManager(document);
	
	this.callback = null;
	this.numFields = 0;
	
	this.conn = null;
	this.currSeiType = null;
	this.currSeiIndex = null;
	
	this.changedFlag = false;
	this.displayedIndex = -1;
	
	this.html = ' \
	<div id="modaldialog_div" style="width:90ch;">\
	<center>\
	<form id="loadDialogForm">\
	<table border="1">\
		<tr> \
			<td valign="top"> <!-- TABLE: Left  -->\
				<center><legend>Profile</legend></center>\
	    		<form class="form-horizontal"> </form> \
				<form class="form-horizontal">\
				<fieldset> \
					<div class="control-group" style="margin-right: 1ch;"><label class="control-label">Name</label>     <div class="controls"><input type="text" class="input-xlarge" id="mdName"></div></div>\
					</div> \
					<div class="control-group" style="margin-right: 1ch;"><label class="control-label">Domain:</label>          <div class="controls"><input title="URI prefix of model" rel="tooltip" type="text" class="input-xlarge"  id="mdDomain"></div></div>\
					<hr style="margin-top: 1ch; margin-bottom: 1ch;">\
					<table width="100%"><tr>\
						<td style="padding: 1ch;"><h3>Graphs: </td> \
						<td style="padding: 1ch;" align="right"> model <span id="mdModelButtonDiv" class="btn-group" float="right"></span></td> \
						<td style="padding: 1ch;" align="right"> data  <span id="mdDataButtonDiv"  class="btn-group" float="right"></span></td> \
					</tr></table> \
					<hr style="margin-top: 1ch; margin-bottom: 2ch;">\
					<div class="control-group" style="margin-right: 1ch;"><label class="control-label">Server URL:</label>      <div class="controls"><input type="text" class="input-xlarge"  id="mdServerURL"></div></div>\
					<div class="control-group" style="margin-right: 1ch;"><label class="control-label">Type:</label><div class="controls" align="left">\
						<select id="mdSelectSeiType"> \
							<option value="F"         >fuseki</label></option>\
							<option value="V" selected>virtuoso</label></option>\
						</select> \
					</div></div> \
					<div class="control-group" style="margin-right: 1ch;"><label class="control-label" id="mdSDS0">Dataset:</label><div class="controls"><input type="text" class="input-xlarge" id="mdDataset" ></div></div>\
					<div class="form-actions" style="padding-top:1ch; padding-bottom:1ch;"  align="right"> \
						<button type="button" class="btn" id="mdSeiDelete">Delete</button>\
					</div>\
				</fieldset>\
				</form>\
			</td>\
			<td valign="top"> <!-- TABLE: Right -->\
				<center><legend>Server Profiles</legend></center>\
				<select id="mdSelectProfiles" size=20 style="min-width:90%; margin-left:1ch; margin-right:1ch">\
				</select>\
				<div class="form-actions" style="padding-top:1ch; padding-bottom:1ch;"  align="right"> \
		            <button type="button" class="btn" id="mdProfileCopy"   >Copy</button>\
					<button type="button" class="btn" id="mdProfileNew"    >New</button>\
					<button type="button" class="btn" id="mdProfileDelete" >Delete</button>\
				</div>\
				<div class="form-actions" style="padding-top:1ch; padding-bottom:1ch;"  align="right"> \
					<button type="button" class="btn" id="mdProfileSaveAll" >Save All</button>\
					<button type="button" class="btn" id="mdProfileImport"  >Import</button>\
					<button type="button" class="btn" id="mdProfileExport"  >Export</button>\
				</div>\
			</td>\
		</table>\
		<div class="form-actions" style="padding-top:1ch; padding-bottom:1ch;"  align="right"> \
			<input type="button" id="mdCancel" class="btn-danger" value="Cancel" ></input> \
		    <input type="submit" class="btn-primary" value="Submit"></input>\
		</div>\
	</form>\
	</center>\
	</div>\
	'.replace(/%VAR/g, varNameOBSOLETE);
	
	this.hide();
};

ModalLoadDialog.COOKIE_NAME = "mdProfile";
ModalLoadDialog.COOKIE_NAME_INDEX = "mdIndex";

ModalLoadDialog.prototype = {
		
	loadDialog : function (curConn, callback) {
		// load dialog   
		// callback(sparqlconnection)
		this.callback = callback;
		this.div.innerHTML = this.html;
		
		$("[rel='tooltip']").tooltip();		
		
		this.show();
		// ==== Callbacks that don't use the (ahem) %VAR trick ====
		
		document.getElementById("mdName").onchange=this.changed.bind(this, true);
		document.getElementById("mdDomain").onchange=this.changed.bind(this, true);
		document.getElementById("mdServerURL").onchange=this.changed.bind(this, true);
		document.getElementById("mdSelectSeiType").onchange=this.changed.bind(this, true);
		document.getElementById("mdDataset").onchange=this.changed.bind(this, true);
		
		document.getElementById("loadDialogForm")  .onsubmit=this.callbackSubmit.bind(this);
		document.getElementById("mdSeiDelete")     .onclick=this.callbackDeleteSei.bind(this);
		document.getElementById("mdSelectProfiles").onchange=this.callbackSelectionChange.bind(this);
		document.getElementById("mdProfileCopy")   .onclick=this.callbackCopy.bind(this);
		document.getElementById("mdProfileNew")    .onclick=this.callbackNew.bind(this);
		document.getElementById("mdProfileDelete") .onclick=this.callbackDeleteProfile.bind(this);
		document.getElementById("mdProfileSaveAll").onclick=this.saveAllProfiles.bind(this);
		document.getElementById("mdProfileImport") .onclick=this.importProfiles.bind(this);
		document.getElementById("mdProfileExport") .onclick=this.exportProfiles.bind(this);
		document.getElementById("mdCancel")        .onclick=this.callbackCancel.bind(this);
		
		this.readProfiles(curConn);
		
		this.changed(false);

	},
		
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
	
	callbackCopy : function() {
		if (this.conn == null) {
			this.callbackNew();
		} else {
			var conn = new SparqlConnection(this.conn.toString());
			conn.setName(this.conn.getName() + " copy");
			this.switchToNewProfile(conn);
		}
	},
	
	callbackNew : function() {
		
		// create a blank profile
		var conn = new SparqlConnection();
		conn.setName("-new-");
		conn.addModelInterface(SparqlConnection.VIRTUOSO_SERVER, "", "");
		conn.addDataInterface(SparqlConnection.VIRTUOSO_SERVER, "", "");
		
		this.switchToNewProfile(conn);
	},
	
	switchToNewProfile : function (conn) {
		this.storeProfileFromScreen();
		this.sortProfiles();
		
		this.conn = conn;
		this.appendProfile(this.conn);
		
		var select = this.document.getElementById("mdSelectProfiles");
		select.selectedIndex = select.options.length-1;
		this.displaySelectedProfile();
		this.changed(true);
	},
	
	callbackDeleteProfile : function () {
		var i = this.getSelectedIndex();
		var select = this.document.getElementById("mdSelectProfiles");
		// do nothing if nothing is selected
		if (i < 0) {
			return;
		}
		select.options[i].selected = false;
		select.remove(i);
		select.selectedIndex = (i < select.options.length) ? i : select.options.length -1 ;
		
		this.displaySelectedProfile();
		this.changed(true);
	},
	
	callbackSubmit : function () {
		// save all the saved profiles and return whatever is showing
		this.hide();
		this.storeProfileFromScreen();
		this.writeProfiles();
		this.callback(this.conn);
	},
	
	callbackCancel : function () {
		if (this.changedFlag && !confirm("Discard changes?") ) {
			return;
		} else {
			this.hide();
		}
		
	},
	
	callbackSelectionChange : function () {
		// selection changes, including to -1
		this.storeProfileFromScreen();
		this.sortProfiles();
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
				return other.getName();
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
	
	changed : function (bool) {
		this.changedFlag = bool;
		this.document.getElementById("mdProfileSaveAll").disabled = !bool;
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
	
	callbackDeleteSei : function() {
		
		
		if (this.currSeiType == "m") {
			this.conn.delModelInterface(this.currSeiIndex);
			
			if (this.conn.getModelInterfaceCount() == 0) {
				// add a 0th interface
				this.conn.addModelInterface(SparqlConnection.VIRTUOSO_SERVER, null, null);
				this.currSeiIndex = 0;
				
			} else if (this.currSeiIndex >= this.conn.getModelInterfaceCount()) {
				// delete button and move to prev
				var button = document.getElementById("mdSeiButton_"+ this.currSeiType + this.currSeiIndex);
				button.parentNode.removeChild(button);
				this.currSeiIndex = this.conn.getModelInterfaceCount() - 1;
			} 
			
		} else {
			this.conn.delDataInterface(this.currSeiIndex);
			
			if (this.conn.getDataInterfaceCount() == 0) {
				// add a 0th interface
				this.conn.addDataInterface(SparqlConnection.VIRTUOSO_SERVER, null, null);
				this.currSeiIndex = 0;
				
			} else if (this.currSeiIndex >= this.conn.getDataInterfaceCount()) {
				// delete button and move to prev
				var button = document.getElementById("mdSeiButton_"+ this.currSeiType + this.currSeiIndex);
				button.parentNode.removeChild(button);
				this.currSeiIndex = this.conn.getDataInterfaceCount() - 1;
			} 
		}
		
		// tell storeSeiFromScreen() there's nothing on the screen to save
		var saveType = this.currSeiType;
		this.currSeiType = null;  
		
		// change to new index
		this.callbackChangeSei(saveType, this.currSeiIndex);
		
		this.changed(true);
		
	},
	
	// pressed "+" button
	callbackAddSei : function(seiType) {
		var div; 
		var plusBut;
		
		// create new button
		
		var i = (seiType == "m") ? this.conn.getModelInterfaceCount() : this.conn.getDataInterfaceCount();
		var button = this.createSeiButton(seiType, i);
		
		// find right list and plus button to insert before
		if (seiType == "m") {
			div = document.getElementById("mdModelButtonDiv");
			plusBut = document.getElementById("mdSeiButtonModelPlus");
			this.conn.addModelInterface(SparqlConnection.VIRTUOSO_SERVER, null, null);
		} else {
			div = document.getElementById("mdDataButtonDiv");
			plusBut = document.getElementById("mdSeiButtonDataPlus");
			this.conn.addDataInterface(SparqlConnection.VIRTUOSO_SERVER, null, null);

		}
		
		// insert
		div.insertBefore(button, plusBut);
		
		this.callbackChangeSei(seiType, i);
		
		this.changed(true);
		
	},
	
	// 
	// save current screen     - unless this.currSeiType is null
	// show the new sei        - (null,-1) just clears everything
	callbackChangeSei : function(seiType, seiIndex) {
		
		// update all button active states
		var buttons = document.getElementById("mdModelButtonDiv").children;
		for (var i=0; i < buttons.length; i++) {
			buttons[i].className = "btn";
		}
		buttons = document.getElementById("mdDataButtonDiv").children;
		for (var i=0; i < buttons.length; i++) {
			buttons[i].className = "btn";
		}
		
		if (seiIndex > -1) {
			document.getElementById("mdSeiButton_"+seiType+seiIndex).className = "btn active";
		}
		
		// save current screen
		this.storeSeiFromScreen();
		
		// save new current sei identifiers
		this.currSeiType = seiType;
		this.currSeiIndex = seiIndex;
		
		// set screen fields from memory
		if (seiIndex > -1) {
			var sei = seiType == "m" ? this.conn.getModelInterface(seiIndex) : this.conn.getDataInterface(seiIndex);
			document.getElementById("mdSelectSeiType").selectedIndex = (sei.getServerType() == SparqlConnection.FUSEKI_SERVER) ? 0 : 1;
			document.getElementById("mdServerURL").value = sei.getServerURL();
			document.getElementById("mdDataset").value = sei.getDataset();
			
			document.getElementById("mdSelectSeiType").disabled=false;
			document.getElementById("mdServerURL").disabled=false;
			document.getElementById("mdDataset").disabled=false;
		} else {
			document.getElementById("mdSelectSeiType").selectedIndex = 1;
			document.getElementById("mdServerURL").value = "";
			document.getElementById("mdDataset").value = "";
			
			document.getElementById("mdSelectSeiType").disabled=true;
			document.getElementById("mdServerURL").disabled=true;
			document.getElementById("mdDataset").disabled=true;
		}
	},
	
	// pull sei fields from screen into current sei
	storeSeiFromScreen : function() {
		
		if (this.currSeiType != null) {
			// get the current sei
			var sei = (this.currSeiType == "m") ? this.conn.getModelInterface(this.currSeiIndex) : this.conn.getDataInterface(this.currSeiIndex);
			
			// set fields
			sei.setServerType(document.getElementById("mdSelectSeiType").value == "F" ? SparqlConnection.FUSEKI_SERVER : SparqlConnection.VIRTUOSO_SERVER);
			sei.setServerURL(this.document.getElementById("mdServerURL").value.trim());
			sei.setDataset(this.document.getElementById("mdDataset").value.trim());
		}
	},
	
	// assemble a button
	createSeiButton : function(butType, butIndex) {
		var button = document.createElement("button");
		button.className = "btn";
		button.id="mdSeiButton_" + butType + butIndex;
		button.innerHTML = butIndex+1;
		button.onclick = this.callbackChangeSei.bind(this, butType, butIndex);
		return button;
	},
	
	// set up the correct number of buttons
	// and populate the screen for current sei
	displaySei : function() {
		// clear state
		this.currSeiType = null;
		this.currSeiIndex = null;
		
		var mCount = (this.conn == null) ? 0 : this.conn.getModelInterfaceCount();
		var dCount = (this.conn == null) ? 0 : this.conn.getDataInterfaceCount();
		
		//if (mCount == 0) { this.conn.addModelInterface( SparqlConnection.VIRTUOSO_SERVER, null, null); }
		//if (dCount == 0) { this.conn.addDataInterface ( SparqlConnection.VIRTUOSO_SERVER, null, null); }

		var toolbar;
		var button;
		
		// add model buttons
		toolbar = document.getElementById("mdModelButtonDiv");
		toolbar.innerHTML = "";
		for (var i=0; i < mCount; i++) {
			toolbar.appendChild(this.createSeiButton("m", i));
		}
		
		// model + button
		button = document.createElement("button");
		button.className = "btn";
		button.id="mdSeiButtonModelPlus";
		button.innerHTML = "+";
		button.onclick = this.callbackAddSei.bind(this, "m");
		button.disabled = (this.conn == null);
		toolbar.appendChild(button);
		
		// add data buttons
		toolbar = document.getElementById("mdDataButtonDiv");
		toolbar.innerHTML = "";
		for (var i=0; i < dCount; i++) {
			toolbar.appendChild(this.createSeiButton("d", i));
		}
		
		// data + button
		button = document.createElement("button");
		button.className = "btn";
		button.id="mdSeiButtonDataPlus";
		button.innerHTML = "+";
		button.onclick = this.callbackAddSei.bind(this, "d");
		button.disabled = (this.conn == null);

		toolbar.appendChild(button);
		
		// display m0
		if (this.conn == null) {
			this.callbackChangeSei(null, -1);
		} else {
			this.callbackChangeSei("m", 0);
		}
		
		
	},
	
	// pull profile into this.conn and display it
	displaySelectedProfile : function() {
		index = this.getSelectedIndex();
		this.displayedIndex = index;
		
		var select = this.document.getElementById("mdSelectProfiles");
		var profileStr = (select.selectedIndex > -1) ? select.options[select.selectedIndex].value : null;
		
		if (profileStr != null) {
			this.conn = new SparqlConnection();
			this.conn.fromString(profileStr);
			
			document.getElementById("mdName").value = this.conn.getName();
			document.getElementById("mdDomain").value = this.conn.getDomain();
			document.getElementById("mdName").disabled = false;
			document.getElementById("mdDomain").disabled = false;
		} else {
			this.conn = null;
			document.getElementById("mdName").value = "";
			document.getElementById("mdDomain").value = "";
			document.getElementById("mdName").disabled = true;
			document.getElementById("mdDomain").disabled = true;
		}
			
		
		this.displaySei();
	},
	
	// store screen to this.conn, then into the select.option[].value
	storeProfileFromScreen : function() {
		if (this.displayedIndex == -1) {
			return;
		}
		
		// create a profile from the ones on the screen
		this.conn.setName(this.document.getElementById("mdName").value.trim());
		
		
		
		var domain = this.document.getElementById("mdDomain").value;
		// Adds the http:// on front of domain if needed
		if (domain.indexOf("http") != 0) {
			domain = "http://" + domain;
		}
		
		this.conn.setDomain(domain);
		
		this.storeSeiFromScreen();
		
		// put into select
		var select = this.document.getElementById("mdSelectProfiles");
		select.options[this.displayedIndex].value = this.conn.toString();
		select.options[this.displayedIndex].text = this.conn.getName();
	},
	
	// appends a connection to the select
	appendProfile : function (conn) {

		var opt = new Option(conn.getName(), conn.toString());
		this.document.getElementById("mdSelectProfiles").appendChild(opt);
	},
	
	
	readProfiles : function (curConn) {

		var i = 0;
		var cookieStr = this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);
		var conn;
		var curIndex = -1;
		var select = this.document.getElementById("mdSelectProfiles");
		
		// load all the connections from cookies
		while (cookieStr != null) {
			conn = new SparqlConnection(cookieStr);
			
			// does this cookied profile match curConn.  
			// 'True' indicates we want to ignore the curConn profile name.
			if (curConn && curConn.equals(conn, true)) {
				curIndex = i;
			}
			this.appendProfile(conn);
			
			i += 1;
			cookieStr = this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);
		} 
		
		this.sortProfiles();
		
		// if there is a connection loaded, make sure it is in the cookies
		if (curConn) {
			if (curIndex > -1) {
				// select the current conn in the list of loaded connections
				select.selectedIndex = curIndex;
			} else {
				// add the current conn to the list of loaded connections and select it
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
		this.displaySelectedProfile();
	},
	
	// sort the profiles select, preserving selectedIndex
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
	
	saveAllProfiles: function () {
		this.storeProfileFromScreen();
		this.sortProfiles();
		this.writeProfiles();
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
		
		this.changed(false);
	}, 
	
	exportProfiles : function () {
		this.storeProfileFromScreen();
		this.sortProfiles();
		
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
		if (text == null) {
			return;
		}
		
		var lines = text.split(/}]}/);
		var conn = null;
		
		for (var i=0; i < lines.length; i++) {
			if (lines[i].length < 1) continue;
			try {
				conn = new SparqlConnection(lines[i] + '}]}');
				this.appendProfile(conn);
			} catch (err) {
				alert("Could not import this line:\n" + lines[i]);
			}
		}
		
		this.sortProfiles();
		this.changed(true);
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