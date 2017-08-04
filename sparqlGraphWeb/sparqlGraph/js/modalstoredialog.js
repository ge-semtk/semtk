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
    'sparqlgraph/js/msiclientnodegroupstore',
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/selecttable',
    'sparqlgraph/js/sparqlgraphjson',

    // shimmed
    'jquery'
    ],

	function(MsiClientNodeGroupStore, ModalIidx, IIDXHelper, SelectTable, SparqlGraphJson, jquery) {
	
		/**
		 *
		 */
		var ModalStoreDialog= function (user, serviceUrl) {
            this.div = null;
            this.selTable = null;
            this.user = user; 
            this.serviceUrl = serviceUrl;
            this.callback = function () {};
		};
		
		
		ModalStoreDialog.prototype = {
            
            // user is sent in at creation or entered during storage of a nodegroup
            // can be "" but never null or undefined.
            getUser : function () {
                return this.user;
            },
            
            show : function () {
                IIDXHelper.showDiv(this.div);
            },
            
            hide : function () {
                IIDXHelper.hideDiv(this.div);
            },
            
            validateCallback : function() {
				var selIndices = this.selTable.getSelectedIndices();
				if (selIndices.length == 0) {
					return "No nodegroups are selected";
				} else {
					return null;
				}
			},
			
			okCallback : function() {
				var idList = this.selTable.getSelectedValues("ID");
				this.callback(idList);
			},
			
			cancelCallback : function() {
                
            },
			
            /**
              * got nodegroup store contents.  Launch dialog.
             **/
            launchNodeGroupDialogCallback : function (multiFlag, resultSet) { 
				if (! resultSet.isSuccess()) {
					ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
				} else {

                    // build this.div with a SelectTable
                    var sortFlag = true;
                    var colList = ["creator", "ID", "creationDate", "comments"];
                    var undefVal = "";
    
                    var widthList = [15, 20, 15, 50];
                    this.selTable = new SelectTable(resultSet.tableGetNamedRows(colList, undefVal, sortFlag),
                                                    colList,
                                                    widthList,
                                                    10,
                                                    multiFlag
                                                    );
                    
                    this.div = document.createElement("div");
                    this.div.appendChild(this.selTable.getTableDom());
                    
                    // launch the modal
                    var m = new ModalIidx();
					m.showOKCancel(	
									this.title,
									this.div,
									this.validateCallback.bind(this),
									this.okCallback.bind(this),
									this.cancelCallback.bind(this),
									"OK",
									90); 
                    
				}
			},
    
            /**
              *  Call nodegroup store to get all nodegroups
              *  Then launch generic dialog with title and callback linked to "OK"
              *  callback(id)
              */
            launchNodeGroupDialog : function (title, callback, multiFlag) {
                this.title = title;
                this.callback = callback;
                
                var mq = new MsiClientNodeGroupStore(this.serviceUrl);
    		    mq.getNodeGroupMetadata(this.launchNodeGroupDialogCallback.bind(this, multiFlag));
            },
            
            /**
              * Recieved nodegroup to load, now load it
              */
            retrieveNodeGroupCallback : function (retrieveCallback, resultSet) { 
				if (! resultSet.isSuccess()) {
					ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
				} else {
					var nodegroupArr = resultSet.getStringResultsColumn("NodeGroup");
					
					if (nodegroupArr.length < 1) {
						ModalIidx.alert("Retrieval Failure", "<b>Failure retrieving nodegroup.</b><br>Diagnostic: getNodeGroupById returned zero rows.");
					} else {
						retrieveCallback(nodegroupArr[0]);
					}
				}
			},
			
            /**
              * load the id
              */
            retrieveNodeGroupOK : function (retrieveCallback, idList) {   
                var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                mq.getNodeGroupById(idList[0], this.retrieveNodeGroupCallback.bind(this, retrieveCallback));
            },
            
            /**
              * External call to retrieve a nodegroup from the store
              */
            launchRetrieveDialog : function (retrieveCallback) {
                this.launchNodeGroupDialog("Retrieve from Nodegroup store",
                                           this.retrieveNodeGroupOK.bind(this, retrieveCallback),
                                           false // no multi
                                          );
            },
            
            /**
              * Callback after deletion
              * @param id is nodegroup just deleted
              * @param idList are additional nodegroups to delete
              */
            deleteNodeGroupCallback : function (id, idList, resultSet) { 
               
                // if there is a failure...
                // PEC TODO: search for "0 triples" is a temporary hack until we have a new results set type
				if (! resultSet.isSuccess() || resultSet.getSimpleResultsHtml().indexOf("0 triples") > -1) {
                    
                    // print results using SimpleResults in case it got that far.  It will default to GeneralResults.
                    var msg = "<b>Failure deleting: " + id + "</b>";
                    msg += "<p>" + resultSet.getSimpleResultsHtml();
                    
                    // stop processing and print list of unprocessed ids
                    if (idList.length > 0) {
                        msg += "<p><p><b>Deletes were not attempted on remaining ids:<br></b>" + idList;
                    }
					ModalIidx.alert("Service failed", msg);
                    
				} else {
                    // if there's more to do
                    if (idList.length > 0) {
                        this.deleteNodeGroupOK(idList);
                    } else {
                        //ModalIidx.alert("Delete Nodegroup", resultSet.getSimpleResultsHtml());
                        ModalIidx.alert("Delete Nodegroup", "Successfully deleted.");
                    }
				}
			},
             /**
              * delete the nodegroup
              */
            deleteNodeGroupOK : function (idList) {   
                var mq = new MsiClientNodeGroupStore(this.serviceUrl);
               
                // delete idList[0] and queue up idList[1..end]
                mq.deleteStoredNodeGroup(idList[0], this.deleteNodeGroupCallback.bind(this, idList[0], idList.slice(1)));
            },
            
            /**
              * External call to delete a nodegroup from the store
              */
            launchDeleteDialog : function () {
                 this.launchNodeGroupDialog("Delete from Nodegroup store",
                                           this.deleteNodeGroupOK.bind(this),
                                            true // multi
                                          );
            },
            
            /**
              * Totally different dialog but functionally related
              * so I'm slamming it into this file
              * -Paul
              */
            launchStoreDialog : function (sgJson, doneCallback) {
                
                var successCallback = function (id, resultSet) { 
                    if (! resultSet.isSuccess()) {
                        ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
                    } else {
                        // store returns nothing
                        //ModalIidx.alert("Store Nodegroup", resultSet.getSimpleResultsHtml());
                        ModalIidx.alert("Store Nodegroup", "Successfully stored " + id);
                        doneCallback();
                    }
                };
			
                var validateCallback = function () { 
                    var id = document.getElementById("sngIdText").value;
                    var comments = document.getElementById("sngCommentsText").value;
                    var creator = document.getElementById("sngCreatorText").value;

                    if (id == null || id.length == 0) {
                        return "Id cannot be null.";
                    } else if (creator == null || creator.length == 0) {
                        return "Creator cannot be null.";
                    } else if (comments == null || comments.length == 0) {
                        return "Comments cannot be null.";
                    } else {
                        return null;
                    }
                };

                var clearCallback = function () { 
                    document.getElementById("sngIdText").value="";
                    document.getElementById("sngCommentsText").value="";
                    document.getElementById("sngCreatorText").value=this.user;

                };

                var submitCallback = function (json) { 
                    var name = document.getElementById("sngIdText").value;
                    var comments = document.getElementById("sngCommentsText").value;
                    var creator = document.getElementById("sngCreatorText").value;
                    
                    var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                    
                    // save user/creator
                    this.user = creator;
                    
                    mq.storeNodeGroup(json, creator, name, comments, successCallback.bind(this, name));
                }.bind(this, sgJson);

                var div = document.createElement("div");
                var form = IIDXHelper.buildHorizontalForm();
                div.appendChild(form);
                var fieldset = IIDXHelper.addFieldset(form)

                fieldset.appendChild(IIDXHelper.buildControlGroup("creator: ", IIDXHelper.createTextInput("sngCreatorText")));
                fieldset.appendChild(IIDXHelper.buildControlGroup("id: ", IIDXHelper.createTextInput("sngIdText")));
                fieldset.appendChild(IIDXHelper.buildControlGroup("comments: ", IIDXHelper.createTextArea("sngCommentsText", 2)));

                var m = new ModalIidx("storeNodeGroupDialog");
                m.showClearCancelSubmit("Save nodegroup to store",
                                        div, 
                                        validateCallback,
                                        clearCallback, 
                                        submitCallback
                                        );
                
                document.getElementById("sngCreatorText").value=this.user;
                
            },
            
		};
	
		return ModalStoreDialog;
	}
);