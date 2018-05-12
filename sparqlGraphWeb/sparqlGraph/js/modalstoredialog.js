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
            this.lastRetrievedId = null;
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
              * load the id
              */
            retrieveNodeGroupOK : function (retrieveCallback, idList) {   
                var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                mq.getNodeGroupByIdToJsonStr(idList[0], retrieveCallback);
                this.lastRetrievedId = idList[0];
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
                // get all existing meta data, and call launchStoreDialog2
                var mq = new MsiClientNodeGroupStore(this.serviceUrl);
    		    mq.getNodeGroupMetadata(this.launchStoreDialog2.bind(this, sgJson, doneCallback));
            },
            
            launchStoreDialog2 : function (sgJson, doneCallback, resultSet) {
                // check that meta data returned
                if (! resultSet.isSuccess()) {
                    ModalIidx.alert("Failure retrieving store contents",
                                    resultSet.getSimpleResultsHtml()
                                   );
                    return;
                    
				}
                
                // storing of dialog succeeded
                var successCallback = function (id, closeModal, resultSet) { 
                    if (! resultSet.isSuccess()) {
                        ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
                    } else {
                        // store returns nothing
                        //ModalIidx.alert("Store Nodegroup", resultSet.getSimpleResultsHtml());
                        ModalIidx.alert("Store Nodegroup", "Successfully stored " + id);
                        closeModal()
                        doneCallback();
                    }
                };

                // user hit "Clear"
                var clearCallback = function () { 
                    document.getElementById("sngIdText").value="";
                    document.getElementById("sngCommentsText").value="";
                    document.getElementById("sngCreatorText").value=this.user;

                };
                
                // make the call to store a nodegoup
                var submitCallbackStore = function (json, closeModal) { 
                    var id = document.getElementById("sngIdText").value;
                    var comments = document.getElementById("sngCommentsText").value;
                    var creator = document.getElementById("sngCreatorText").value;
                    
                    var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                    
                    // save user/creator
                    this.user = creator;
                    this.lastRetrievedId = id;
                    mq.storeNodeGroup(json, creator, id, comments, successCallback.bind(this, id, closeModal));
                }.bind(this);
                
                // delete nodegroup and submit if successful
                var submitCallbackDeleteAndStore = function (id, json, closeModal) {
                    var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                    mq.deleteStoredNodeGroup(id, submitCallbackStore.bind(this, json, closeModal));
                };
                
                // user hit "Submit"
                // check everything, then start callback chain
                var valSubmitCallback = function (rs, json, closeModal) { 
                    var id = document.getElementById("sngIdText").value;
                    var comments = document.getElementById("sngCommentsText").value;
                    var creator = document.getElementById("sngCreatorText").value;

                    if (id == null || id.length == 0) {
                        ModalIidx.alert("Validation error", "ID cannot be empty.");
                    } else if (creator == null || creator.length == 0) {
                        ModalIidx.alert("Validation error", "Creator cannot be empty.");
                    } else if (comments == null || comments.length == 0) {
                        ModalIidx.alert("Validation error", "Comments cannot be empty.");
                    } else if (rs.getStringResultsColumn("ID").indexOf(id) > -1) {
                        // delete and store if user agrees
                        ModalIidx.okCancel("Nodegroup already exists", 
                                           "Do you want to overwrite " + id + " in the store?",
                                            submitCallbackDeleteAndStore.bind(this, id, json, closeModal));
                    } else {
                        // simple store
                        submitCallbackStore(json, closeModal);
                    }
                }.bind(this, resultSet, sgJson);
                
                // update comment based on id
                var updateMetaData = function (rs) {
                    var creatorTxt = document.getElementById("sngCreatorText");
                    var idTxt = document.getElementById("sngIdText");
                    var commentTxt = document.getElementById("sngCommentsText");
                    var ngCreator = "";
                    
                    // search for ID
                    var idCol = rs.getColumnNumber("ID");
                    for (var i=0; i < rs.getRowCount(); i++) {
                        if (rs.getRsData(i, idCol) == idTxt.value) {
                            // update comments on screen
                            commentTxt.value = rs.getRsData(i, rs.getColumnNumber("comments"));
                            // save copy of creator
                            ngCreator = rs.getRsData(i, rs.getColumnNumber("creator"));
                            break;
                        }
                    }
                    
                    // leave comments unchanged when id changes
                    
                    // handle creator
                    var creatorGroup = document.getElementById("sngCreatorGroup"); 
                    if (ngCreator != "" && ngCreator != this.user) {
                       IIDXHelper.changeControlGroupHelpText(creatorGroup, 
                                                             "last saved by " + ngCreator, 
                                                             "warning");
                    } else {
                       IIDXHelper.changeControlGroupHelpText(creatorGroup, "", "");
                    }
                }.bind(this, resultSet);
                
                // build form
                var div = document.createElement("div");
                var form = IIDXHelper.buildHorizontalForm();
                div.appendChild(form);
                var fieldset = IIDXHelper.addFieldset(form)
                
                // build idListElem and idText
                resultSet.sort("ID");
                var idListElem = IIDXHelper.createDataList("storeIDs", resultSet.getStringResultsColumn("ID"));
                div.appendChild(idListElem);
                var idText =      IIDXHelper.createTextInput("sngIdText", "input-xlarge", idListElem);
                idText.onchange = updateMetaData;
                idText.value =      this.lastRetrievedId;
                
                // build the simple inputs
                var commentText = IIDXHelper.createTextArea("sngCommentsText", 2);
                var creatorText = IIDXHelper.createTextInput("sngCreatorText");
                
                // arrange the form
                fieldset.appendChild(IIDXHelper.buildControlGroup("nodegroup id: ", idText));
                fieldset.appendChild(IIDXHelper.buildControlGroup("comments: ", commentText));
                
                var creatorGroup = IIDXHelper.buildControlGroup("my id: ", creatorText);
                creatorGroup.id = "sngCreatorGroup";
                fieldset.appendChild(document.createElement("hr"));
                fieldset.appendChild(creatorGroup);
                
                // launch
                var m = new ModalIidx("storeNodeGroupDialog");
                m.showClearCancelValSubmit("Save nodegroup to store",
                                        div, 
                                        clearCallback, 
                                        valSubmitCallback
                                        );
                updateMetaData();
                
                document.getElementById("sngCreatorText").value=this.user;
                
            },
            
		};
	
		return ModalStoreDialog;
	}
);