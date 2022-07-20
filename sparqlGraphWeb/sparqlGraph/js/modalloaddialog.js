/**
 ** Copyright 2016-19 General Electric Company
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


define([	// properly require.config'ed

            'sparqlgraph/js/iidxhelper',
            'sparqlgraph/js/modalconnwizarddialog',
			'sparqlgraph/js/modaliidx',
			'sparqlgraph/js/msiclientquery',
			
         	'jquery',
            'sparqlgraph/js/cookiemanager',
            'sparqlgraph/js/sparqlconnection',
    		'sparqlgraph/js/sparqlserverinterface'
		],
       function(IIDXHelper, ModalConnWizardDialog, ModalIidx, MsiClientQuery, $) {

    var ModalLoadDialog = function(document, varNameOBSOLETE, ngClient, queryUrl) {

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

        // WOW: very old-school.  Oh well.
        this.html = ' \
        <div id="modaldialog_div" style="width:90ch;">\
        <center>\
        <form id="loadDialogForm">\
        <table border="1">\
            <tr> \
                <td valign="top"> <!-- TABLE: Left -->\
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
                <td valign="top"> <!-- TABLE: Right  -->\
                    <center><legend>Profile</legend></center>\
                    <form class="form-horizontal"> </form> \
                    <form class="form-horizontal">\
                    <fieldset> \
                        <div class="control-group" style="margin-right: 1ch;">                  <label class="control-label">Name:</label>                 <div class="controls">                 <input type="text" class="input-xlarge" id="mdName"></div></div>\
                        </div> \
                        <div class="control-group" style="margin-right: 1ch;" id="mdDomainDiv"> <label class="control-label">Domain:</label>               <div class="controls"><input type="text" class="input-medium"  id="mdDomain"> <button id="mdDomainClearBut" class="btn">Clear</button> <button id="mdDomainInfoBut" class="icon-white btn-small btn-info"><icon class="icon-info-sign"></icon></button></div></div>\
                        <div class="control-group" style="margin-right: 1ch;">                  <label class="control-label">Enable OWL imports:</label>   <div class="controls">                 <input type="checkbox" class="input-xlarge" id="mdOwlImports"></div></div>\
                        <hr style="margin-top: 1ch; margin-bottom: 1ch;">\
                        <table width="100%"><tr>\
                            <td style="padding: 1ch;"><h3>Graphs: </td> \
                            <td style="padding: 1ch;" align="right"> model <span id="mdModelButtonDiv" class="btn-group" float="right"></span></td> \
                            <td style="padding: 1ch;" align="right"> data  <span id="mdDataButtonDiv"  class="btn-group" float="right"></span></td> \
                        </tr></table> \
                        <hr style="margin-top: 1ch; margin-bottom: 2ch;">\
                        <div class="control-group" style="margin-right: 1ch;"><label class="control-label">Server URL:</label>      <div class="controls"><input type="text" class="input-xlarge"  id="mdServerURL" list="mdDatalistServers"></div></div>\
                        <div class="control-group" style="margin-right: 1ch;"><label class="control-label">Type:</label><div class="controls" align="left">\
                            <select id="mdSelectSeiType"> \
                                %%OPTIONS%%\
                            </select> \
                        </div></div> \
                        <div class="control-group" style="margin-right: 1ch;"><label class="control-label" id="mdSDS0">Graph:</label><div class="controls"><input type="text" class="input-large" id="mdGraph"> <button id="mdButAddGraph" class="btn">>></button></div></div>\
                        <div class="control-group" style="margin-right: 1ch;"><label class="control-label" id="mdSDS1">Usage info:</label><div class="controls"><span class="label" id="mdSeiInfo"></span></div></div>\
                        <div class="form-actions" style="padding-top:1ch; padding-bottom:1ch;"  align="right"> \
                            <button type="button" class="btn" id="mdSeiDelete">Delete</button>\
                        </div>\
                    </fieldset>\
                    </form>\
                </td>\
            </table>\
            <div class="form-actions" style="padding-top:1ch; padding-bottom:1ch;"  align="right"> \
                <input type="checkbox" id="mdNoCacheCheckbox" style="margin: 0"><span style="padding-right:4ch;"> clear cache</span> \
                <input type="button" id="mdCancel" class="btn" value="Cancel" ></input> \
                <input type="submit" class="btn btn-primary" value="Submit"></input>\
            </div>\
        </form>\
        </center>\
        </div>\
        <datalist id="mdDatalistServers"></datalist>\
        <datalist id="mdDatalistGraphs"></datalist>\
        '.replace(/%VAR/g, varNameOBSOLETE);

        gotServerTypes = function(results) {
            if (results.isSuccess()) {
                this.serverTypeList = results.getSimpleResultField("serverTypes");
                var html = "";
                for (var t of  this.serverTypeList) {
                    if (t == "fuseki") {
                        html += "<option selected>" + t + "</option>";
                    } else {
                        html += "<option>" + t + "</option>";
                    }
                }
                this.html = this.html.replace("%%OPTIONS%%", html);
            } else {
                ModalIidx.alert("Error", results.getFailureHtml(), false);
            }
        }.bind(this);

        ngClient.execGetServerTypes(gotServerTypes);
		this.queryUrl = queryUrl;
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
            document.getElementById("mdName").onchange         =this.callbackChangedName.bind(this);
            document.getElementById("mdDomain").onchange       =this.callbackChangedDomain.bind(this);
            document.getElementById("mdDomainClearBut").onclick   =this.callbackDomainClear.bind(this);
            document.getElementById("mdDomainInfoBut").onclick =this.callbackDomainInfo.bind(this);
            document.getElementById("mdDomain").onchange       =this.callbackChangedDomain.bind(this);
            document.getElementById("mdOwlImports").onchange   =this.callbackChangedOwlImports.bind(this);
            document.getElementById("mdServerURL").onchange    =this.callbackChangedServerURL.bind(this);
            document.getElementById("mdSelectSeiType").onchange=this.callbackChangedSelectSeiType.bind(this);
            document.getElementById("mdGraph").onchange        =this.callbackChangedGraph.bind(this);
            document.getElementById("mdButAddGraph").onclick   =this.callbackAddGraph.bind(this);

            document.getElementById("loadDialogForm")  .onsubmit=this.callbackSubmit.bind(this);
            document.getElementById("mdSeiDelete")     .onclick =this.callbackDeleteSei.bind(this);
            document.getElementById("mdSelectProfiles").onchange=this.callbackSelectionChange.bind(this);
            document.getElementById("mdProfileCopy")   .onclick =this.callbackCopy.bind(this);
            document.getElementById("mdProfileNew")    .onclick =this.callbackNew.bind(this);
            document.getElementById("mdProfileDelete") .onclick =this.callbackDeleteProfile.bind(this);
            document.getElementById("mdProfileSaveAll").onclick =this.callbackSaveAll.bind(this);
            document.getElementById("mdProfileImport") .onclick =this.callbackImportProfiles.bind(this);
            document.getElementById("mdProfileExport") .onclick =this.callbackExportProfiles.bind(this);
            document.getElementById("mdCancel")        .onclick =this.callbackCancel.bind(this);

            this.readProfiles(curConn);

            this.changed(false);
            
            // If there are no profiles, automatically hit the "new" button
            if (this.document.getElementById("mdSelectProfiles").options.length == 0) {
				this.callbackNew();
			}

        },

        callbackDomainClear : function() {
            document.getElementById("mdDomain").value="";
            return false;
        },

        callbackDomainInfo : function() {
            var msgHtml = "Domain is a deprecated regex matching URI prefixes of T-box classes<br>" +
                          "Domains are still honored by SemTK but can no longer be created.<br>" +
                          "It is recommended that you click <strong>Clear</strong> to clear the domain.";

            ModalIidx.alert("Domain is deprecated", msgHtml, false, function(){});
            return false;
        },

        // changes don't store anything
        callbackChangedName : function() {
            var select = this.document.getElementById("mdSelectProfiles");
            select[this.displayedIndex].text = document.getElementById("mdName").value.trim();
            this.sortProfiles();

            this.changed(true);
            return false;
        },
        callbackChangedDomain : function() {
            this.changed(true);
            return false;
        },
        callbackChangedOwlImports : function() {

            this.changed(true);
            return false;
        },
        callbackChangedServerURL : function() {
            this.changed(true);
            return false;
        },
        callbackChangedSelectSeiType : function() {
            this.changed(true);
            return false;
        },
        callbackChangedGraph : function() {
            this.changed(true);
            return false;
        },
        
        callbackAddGraph : function() {
	
	  		var serverType = IIDXHelper.getSelectValues(document.getElementById("mdSelectSeiType"))[0];
            var serverUrl =  this.document.getElementById("mdServerURL").value.trim();
	
			var queryClient = new MsiClientQuery(this.queryUrl, new SparqlServerInterface(serverType, serverUrl, "http://any#graph"));
			queryClient.execQuery("SELECT ?g WHERE { GRAPH ?g { }}", this.callbackAddGraph2.bind(this));
			return false;
		},

		callbackAddGraph2 : function(results) {
			if (!results.isSuccess()) {
				ModalIidx.alert("Error retrieving graphs", "Server URL and/or type are likely invalid.<hr><b>Message:</b><br>" + results.getStatusMessage());
				
			} else {
				var graphs = results.getColumnStrings(0).sort();
				var callback = function(item) {document.getElementById("mdGraph").value = item; };
				ModalIidx.listDialog("Choose graph", "ok", graphs, graphs, 0, callback, undefined, undefined, true);
			}
		},
		
        callbackCopy : function() {
            if (this.conn == null) {
                this.callbackNew();
            } else {
                var conn = new SparqlConnection(this.conn.toString());
                conn.setName(this.conn.getName() + " copy");
                this.appendAndSelectProfile(conn);
            }
            return false;
        },

        callbackNew : function() {
			var wiz = new ModalConnWizardDialog(this.conn, this.serverTypeList, this.queryUrl, this.appendAndSelectProfile.bind(this));
			wiz.launch();
            return false;
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
            return false;
        },

        callbackSubmit : function () {
            // save all the saved profiles and return whatever is showing
            var success = function () {
                this.hide();
                this.writeProfiles();
                this.callback(this.conn, function() {}, document.getElementById('mdNoCacheCheckbox').checked);
            }.bind(this);

            this.storeDisplayedProfile();
            this.validateThisConnAsync(success);

            return false;
        },

        callbackCancel : function () {
            if (this.changedFlag && !confirm("Discard changes?") ) {
                return;
            } else {
                this.hide();
            }
            return false;
        },

        callbackSelectionChange : function () {
            // selection changes, including to -1

            var success = function () {
                this.displaySelectedProfile();
            }.bind(this);

            var cancel = function () {
                var select = this.document.getElementById("mdSelectProfiles");
                IIDXHelper.selectFirstMatchingText(select, this.conn.getName());
            }.bind(this);

            this.storeDisplayedProfile();

            // nothing to do unless there is a connection in the select
            if (this.conn != null) {
                this.validateThisConnAsync(success, cancel);
            }
            return false;
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

            // tell storeDisplayedSei() there's nothing on the screen to save
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
            return false;

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
            this.storeDisplayedSei();

            // save new current sei identifiers
            this.currSeiType = seiType;
            this.currSeiIndex = seiIndex;

            // set screen fields from memory
            if (seiIndex > -1) {
                var sei = seiType == "m" ? this.conn.getModelInterface(seiIndex) : this.conn.getDataInterface(seiIndex);
                var selectType = document.getElementById("mdSelectSeiType");
                for (var i=0; i < selectType.options.length; i++) {
                    if (selectType.options[i].text == sei.getServerType()) {
                        selectType.selectedIndex = i;
                    }
                }
                document.getElementById("mdServerURL").value = sei.getServerURL();
                document.getElementById("mdGraph").value = sei.getGraph();

                document.getElementById("mdSelectSeiType").disabled=false;
                document.getElementById("mdServerURL").disabled=false;
                document.getElementById("mdGraph").disabled=false;
                document.getElementById("mdSeiDelete").disabled=false;
                document.getElementById("mdButAddGraph").disabled=false;

                var label = document.getElementById("mdSeiInfo");

                if (seiType == "m") {
                    label.className = "label label-inverse";
                    label.innerHTML = "Read model";
                } else if (seiIndex == 0) {
                    label.className = "label label-success";
                    label.innerHTML = "Select / Insert / Delete";
                } else {
                    label.className = "label label-inverse";
                    label.innerHTML = "Select-only";
                }
            } else {
                document.getElementById("mdSelectSeiType").selectedIndex = 1;
                document.getElementById("mdServerURL").value = "";
                document.getElementById("mdGraph").value = "";

                document.getElementById("mdSelectSeiType").disabled=true;
                document.getElementById("mdServerURL").disabled=true;
                document.getElementById("mdGraph").disabled=true;
                document.getElementById("mdSeiDelete").disabled=true;
                document.getElementById("mdButAddGraph").disabled=true;

                var label = document.getElementById("mdSeiInfo");

                label.className = "label";
                label.innerHTML = "";
            }

            this.updateDatalists();
            return false;
        },

        // update the datalists for serverURL and Graph
        // with the values from the other Sei's
        updateDatalists : function () {
            var servers = document.getElementById("mdDatalistServers");
            var graphs = document.getElementById("mdDatalistGraphs");
            var serverArr = [];
            var graphArr = [];

            // clear lists
            servers.innerHTML = "";
            graphs.innerHTML = "";

            if (this.conn != null) {
                // add all Model Interfaces' ServerURL's and Graphs
                for (var i=0; i < this.conn.getModelInterfaceCount(); i++) {
                    var s = this.conn.getModelInterface(i).getServerURL();
                    var d = this.conn.getModelInterface(i).getGraph();

                    if (s!= null && s != "" && serverArr.indexOf(s) == -1) {
                        serverArr.push(s);
                        servers.innerHTML += '<option value="' + s + '">';
                    }

                    if (d != null && d != "" && graphArr.indexOf(d) == -1) {
                        graphArr.push(d);
                        graphs.innerHTML += '<option value="' + d + '">';
                    }
                }
                // repeat for Data Interfaces
                for (var i=0; i < this.conn.getDataInterfaceCount(); i++) {
                    var s = this.conn.getDataInterface(i).getServerURL();
                    var d = this.conn.getDataInterface(i).getGraph();

                    if (s!= null && s != "" && serverArr.indexOf(s) == -1) {
                        serverArr.push(s);
                        servers.innerHTML += '<option value="' + s + '">';
                    }

                    if (d != null && d != "" && graphArr.indexOf(d) == -1) {
                        graphArr.push(d);
                        graphs.innerHTML += '<option value="' + d + '">';
                    }
                }
            }

        },

        callbackExportProfiles : function () {

            var success = function() {
                var result = this.getAllProfilesString();
                this.downloadFile(result, "profiles.txt");
            }.bind(this);

            this.storeDisplayedProfile();
            this.validateThisConnAsync(success);
            return false;
        },

        callbackImportProfiles : function () {
            var text = prompt("Paste profiles here:", " ");
            if (text == null) {
                return;
            }

            this.appendProfilesString(text);

            this.sortProfiles();
            this.changed(true);
            return false;
        },

        appendAndSelectProfile : function (conn) {

            var success = function() {
                this.conn = conn;
                this.appendProfile(this.conn);

                var select = this.document.getElementById("mdSelectProfiles");
                select.selectedIndex = select.options.length-1;
                this.sortProfiles();

                this.displaySelectedProfile();
                this.changed(true);
            }.bind(this);

            this.storeDisplayedProfile();
            if (this.conn) {
                this.validateThisConnAsync(success);
            } else {
                success();
            }
        },

        // External
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
                        this.conn = conn;
                        this.writeCurrProfile();
                    }
                    return other.getName();
                }
            }
            return false;
        },

        // External
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

        // assemble a button
        createSeiButton : function(butType, butIndex) {
            var button = document.createElement("button");
            button.className = "btn";
            button.id="mdSeiButton_" + butType + butIndex;
            button.innerHTML = butIndex+1;
            button.onclick = this.callbackChangeSei.bind(this, butType, butIndex);
            return button;
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
                document.getElementById("mdOwlImports").checked = this.conn.isOwlImportsEnabled();
                document.getElementById("mdName").disabled = false;
                document.getElementById("mdDomain").disabled = true;    // deprecated, always disabled
                document.getElementById("mdOwlImports").disabled = false;

                var domain = this.conn.getDomain();
                if (domain && domain.length > 0) {
                    document.getElementById("mdDomainDiv").style.display="";
                } else {
                    document.getElementById("mdDomainDiv").style.display="none";
                }
            } else {
                this.conn = null;
                document.getElementById("mdName").value = "";
                document.getElementById("mdDomain").value = "";
                document.getElementById("mdOwlImports").value = false;
                document.getElementById("mdName").disabled = true;
                document.getElementById("mdDomain").disabled = true;
                document.getElementById("mdOwlImports").disabled = true;

                document.getElementById("mdDomainDiv").style.display="none";
            }


            this.displayProfileSei();
        },

        // set up the correct number of buttons
        // and populate the screen for current sei
        displayProfileSei : function() {
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

        // Remove empty sei's
        // Except model[0] (keep regardless)
        // and data[0] (copy model[0] if it's empty
        //
        cleanupConnection : function (cn) {
            // If dataInterface[0] is empty then duplicate modelInterface[0]
            var data0 = cn.getDataInterface(0);
            if (data0.getServerURL() == "" && data0.getGraph() == "") {
                var model0 = cn.getModelInterface(0);
                data0.setServerURL(model0.getServerURL());
                data0.setGraph(model0.getGraph());
            };

            // remove empty sei's with indices > 0
            for (var i=cn.getDataInterfaceCount()-1; i > 0; i--) {
                var sei = cn.getDataInterface(i);
                if (sei.getServerURL() == "" && sei.getGraph() == "") {
                    cn.delDataInterface(i);
                }
            }
            for (var i=cn.getModelInterfaceCount()-1; i > 0; i--) {
                var sei = cn.getModelInterface(i);
                if (sei.getServerURL() == "" && sei.getGraph() == "") {
                    cn.delModelInterface(i);
                }
            }
        },

        /*
         * Check this.conn and call the successCallback if either:
         *   - there are no errors
         *   - user says to save it anyway
         */
        validateThisConnAsync : function (successCallback, optCancelCallback) {
            var errHTML = "";
            var header = "<b>Connection error:</b><br>";

            if (this.conn == null) {
                errHTML = "No connections have been built.<br>";
            } else {
                // look for errors
                if (this.conn.getName() == "") {
                    errHTML += "Name is empty. <br>";
                } else {
                    header = "<b>Connection '" + this.conn.getName() + "' has the following errors:</b><br>";
                }

                for (var i=0; i < this.conn.getModelInterfaceCount(); i++) {
                    var sei = this.conn.getModelInterface(i);
                    if (sei.getServerURL() == "") {
                        errHTML += "Ontology endpoint " + i + " server URL is empty. <br>";
                    }
                    if (sei.getGraph() == "") {
                        errHTML += "Ontology endpoint " + i + " graph is empty. <br>";
                    } else if (sei.getGraph().indexOf(":") == -1 && sei.getServerType() == SparqlConnection.VIRTUOSO_SERVER ) {
                        errHTML += "Ontology endpoint " + i + " graph does not contain ':'<br>";
                    }
                }

                for (var i=0; i < this.conn.getDataInterfaceCount(); i++) {
                    var sei = this.conn.getDataInterface(i);
                    if (sei.getServerURL() == "") {
                        errHTML += "Data endpoint " + i + " server URL is empty. <br>";
                    }

                    if (sei.getGraph() == "") {
                        errHTML += "Data endpoint " + i + " graph is empty. <br>";
                    } else if (sei.getGraph().indexOf(":") == -1 && sei.getServerType() == SparqlConnection.VIRTUOSO_SERVER  ) {
                        errHTML += "Data endpoint " + i + " graph does not contain ':'<br>";
                    }
                }
            }

            // call one of the callbacks
            if (errHTML == "") {
                successCallback();
            } else {

                ModalIidx.okCancel("Connection error",
                                   header + errHTML,
                                   successCallback,
                                   "Continue",
                                   optCancelCallback);
            }

        },

        // store screen to this.conn, then into the select.option[].value
        storeDisplayedProfile : function() {
            if (this.displayedIndex == -1) {
                return;
            }

            // put non-sei fields into this.conn
            this.conn.setName(this.document.getElementById("mdName").value.trim());
            this.conn.setDomain(this.document.getElementById("mdDomain").value.trim());
            this.conn.setOwlImportsEnabled(this.document.getElementById("mdOwlImports").checked);

            // put sei into this.conn
            this.storeDisplayedSei();

            // put this.conn into select[n]
            var select = this.document.getElementById("mdSelectProfiles");
            select.options[this.displayedIndex].value = this.conn.toString();
            select.options[this.displayedIndex].text = this.conn.getName();
        },

        // pull sei fields from screen into this.conn
        // if data or model has no sei, then cheat and copy this one there too.
        storeDisplayedSei : function() {

            if (this.currSeiType != null) {
                // get the current sei
                var sei = (this.currSeiType == "m") ? this.conn.getModelInterface(this.currSeiIndex) : this.conn.getDataInterface(this.currSeiIndex);
                var oppSei0 = (this.currSeiType == "m") ? this.conn.getDataInterface(0) : this.conn.getModelInterface(0);

                // set fields
                var serverTypeSelect = document.getElementById("mdSelectSeiType");
                sei.setServerType(serverTypeSelect.options[serverTypeSelect.selectedIndex].text);
                sei.setServerURL(this.document.getElementById("mdServerURL").value.trim());
                sei.setGraph(this.document.getElementById("mdGraph").value.trim());

                // of Other type of sei's 0th entry is empty, copy this one in.
                if (oppSei0.getServerURL() == "" && oppSei0.getGraph() == "") {
                    oppSei0.setServerURL(sei.getServerURL());
                    oppSei0.setServerType(sei.getServerType());
                    oppSei0.setGraph(sei.getGraph());

                }
            }
        },

        // appends a connection to the select
        appendProfile : function (conn) {

            // change to string and store it in the select
            var select = this.document.getElementById("mdSelectProfiles");
            var origName = conn.getName();
            var name = origName;
            var i=0;

            while (IIDXHelper.selectContainsText(select, name)) {
                i += 1;
                name = origName + i;
            }
            if (name != origName) {
                conn.setName(name);
            }

            var opt = new Option(conn.getName(), conn.toString());
            this.document.getElementById("mdSelectProfiles").appendChild(opt);
        },



        // while remaining hidden, return last loaded conn or null
        getLastConnectionInvisibly : function() {
            var lastConn =  localStorage.getItem("SPARQLgraph_curProfile");

            /**** handle conversion from deprecated cookles to new localStorage ***/
            if (lastConn == null) {
                var depConn = this.getLastConnectionInvisiblyCookiesDEPRECATED();
                if (depConn != null) {
                    this.cookieManager.delCookie(ModalLoadDialog.COOKIE_NAME_INDEX);
                    return depConn;
                }
            }
            /**** end deprecation handling ****/

            return (lastConn == null ? null : new SparqlConnection(lastConn));
        },

        readProfiles : function (curConn) {

            var allProfilesStr = localStorage.getItem("SPARQLgraph_allProfiles");
            var select = this.document.getElementById("mdSelectProfiles");

            /**** handle conversion from deprecated cookies to new localStorage ***/
            if (allProfilesStr == null) {
                var i=0;
                var cookieStr = this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);
                allProfilesStr = "";
                // load all the connections from cookies, and delete
                while (cookieStr != null) {
                    allProfilesStr = allProfilesStr + cookieStr;
                    this.cookieManager.delIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);

                    i = i+ 1;
                    cookieStr = this.cookieManager.getIndexedCookie(ModalLoadDialog.COOKIE_NAME, i);
                }
            }
            /**** end deprecation handling ****/

            this.appendProfilesString(allProfilesStr);

            // if there is a connection loaded, make sure it is in the cookies
            if (curConn) {
                // try to find and select currConn, but if it fails then
                if (! this.selectConnection(curConn)) {

                    // add the current conn to the list of loaded connections and select it
                    this.appendProfile(curConn);
                    select.options.length - 1;
                }

            // if no connection loaded, load the one from last session
            } else {
                this.selectConnection(this.getLastConnectionInvisibly());
            }

            this.sortProfiles();
            this.displaySelectedProfile();
        },

        // selects a connection in the select element
        // returns true if found
        selectConnection : function(conn) {
            var select = this.document.getElementById("mdSelectProfiles");

            if (conn != null) {

                for (var i=0; i < select.options.length; i++) {
                    var opt = new SparqlConnection(select.options[i].value);
                    if (conn.equals(opt, true)) {
                        select.selectedIndex = i;
                        return true;
                    }
                }

            }
            return false;
        },

        // sort the profiles select, preserving selectedIndex
        sortProfiles : function () {
            var select = this.document.getElementById("mdSelectProfiles");

            // do nothing if select is empty
            if ( ! select[this.displayedIndex]) {
                return;
            }

            // check for selectedIndex
            var selectedValue = null;
            if (select.selectedIndex > -1) {
                selectedValue = select[select.selectedIndex].value;
            }
            var newSelectedIndex=-1;

            // check for displayedIndex
            var displayedValue = null;
            if (this.displayedIndex > -1) {
                displayedValue = select[this.displayedIndex].value;
            }
            var newDisplayedIndex =-1;

            // make a tmp Array
            var tmpAry = new Array();
            for (var i=0;i<select.options.length;i++) {
                tmpAry[i] = new Array();
                tmpAry[i][0] = select.options[i].text;
                tmpAry[i][1] = select.options[i].value;
            }

            // sort
            tmpAry.sort();

            // clear
            while (select.options.length > 0) {
                select.options[0] = null;
            }

            // add sorted
            for (var i=0;i<tmpAry.length;i++) {
                var op = new Option(tmpAry[i][0], tmpAry[i][1]);
                select.options[i] = op;
                if (tmpAry[i][1] == selectedValue)
                    newSelectedIndex= i;
                if (tmpAry[i][1] == displayedValue)
                    newDisplayedIndex= i;
            }

            // fix selectedIndex and displayedIndex
            select.selectedIndex = newSelectedIndex;
            this.displayedIndex = newDisplayedIndex;
            return;
        },

        // GUI call to save all the profiles
        callbackSaveAll: function () {

            var success = function () {
                this.writeProfiles();
                this.changed(false);
            }.bind(this);

            this.storeDisplayedProfile();
            this.validateThisConnAsync(success);
        },

        /***   DEPRECATED COOKIES ***/
        getLastConnectionInvisiblyCookiesDEPRECATED : function() {
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

        readProfilesFromCookiesDEPRECATED : function (curConn) {

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
        writeProfilesToCookiesDEPRECATED : function () {
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

        /***** End DEPRECATED COOKIES *****/



        writeProfiles : function () {
            // save profiles in local storage

            var select = this.document.getElementById("mdSelectProfiles");

            localStorage.setItem("SPARQLgraph_allProfiles", this.getAllProfilesString());

            this.writeCurrProfile();
        },

        writeCurrProfile : function () {
            if (this.conn != null) {
                localStorage.setItem("SPARQLgraph_curProfile", this.conn.toString());
            } else {
                localStorage.removeItem("SPARQLgraph_curProfile");
            }
        },

        // build a string representing all profiles in the select
        // apply cleanupConnection() to each to tidy up messes the GUI allows during editing
        getAllProfilesString : function () {
            var result = "";

            var select = this.document.getElementById("mdSelectProfiles");

            // loop through profiles and save to cookieManager
            for (var i=0; i < select.length; i++) {
                var conn = new SparqlConnection(select.options[i].value);
                this.cleanupConnection(conn);

                result += conn.toString() + "\n";
            }

            return result;
        },

        // append profiles from string to profiles
        // No sorting, etc., just appendProfile() to the select
        appendProfilesString : function (profilesStr) {
            if (profilesStr == null) {
                return;
            }
            var lines = profilesStr.split(/}]}/);
            var conn = null;

            for (var i=0; i < lines.length; i++) {
                if (lines[i].trim().length < 1) continue;
                try {
                    conn = new SparqlConnection(lines[i] + '}]}');
                    this.appendProfile(conn);
                } catch (err) {
                    console.log(err.stack);
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

    return ModalLoadDialog;            // return the constructor
});
