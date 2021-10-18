define([	// properly require.config'ed
            'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/iidxhelper',

        	'jquery',

            'visjs/vis.min'
			// shimmed

		],
        function(ModalIidx, IIDXHelper, $, vis) {

    var VisJsHelper = function () {
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
                data: {color:{background:'white'}, shape: 'box'}
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

    VisJsHelper.addJsonLdObject = function(j, nodeDict, edgeList) {

        // add the main node
        var shortType = VisJsHelper.getShortType(j);
        var groupVal = VisJsHelper.getLongType(j);
        if (groupVal.indexOf("XMLSchema") > -1) {
            groupVal = "data";
        }

        // add node to nodeDict, potentially overwriting any place-holder
        var id = j["@id"];
        nodeDict[id] = {
            id : id,
            label: shortType,
            title: id,
            group: groupVal
        };

        console.log(groupVal);
        // handle slightly buggy behavior in visJs:
        // if potentially changing the node's group from a known group back to a useDefaultGroups
        // (this happens if node was originally a data property but now we run across it in the JSON-LD)
        // then reset the shape manually or visJs will leave it custom.
        if (groupVal != "data") {
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
                        // add edge to a jsonLd object
                        console.log(j["@id"]+"-"+predName+"-"+objectId);

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
                        var typ = VisJsHelper.getLongType(o);

                        // add data property node
                        // if it later turns out that this is a node (fuseki's JSON_LD format)
                        // it will be overwritten later at the logic above "add node to nodeDict"
                        nodeDict[val] = {
                            id : val,
                            label: val,
                            title: typ,
                            group: "data"
                        };

                        // add the edge
                        console.log(j["@id"]+"-"+predName+"-"+val);
                        edgeList.push({
                            id: j["@id"]+"-"+predName+"-"+val,   // prevent duplicate edges
                            from: j["@id"],
                            to: val,
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

    /*
     * create a unique id with base name
     */
    VisJsHelper.test = function(base) {

    };
    return VisJsHelper;
});
