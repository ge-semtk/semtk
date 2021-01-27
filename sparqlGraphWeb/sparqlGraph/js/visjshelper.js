define([	// properly require.config'ed

        	'jquery',

            'visjs/vis.min'
			// shimmed

		],
        function($, vis) {

    var VisJsHelper = function () {
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
            },
            manipulation: {
                initiallyActive: false,
                deleteNode: true,
                deleteEdge: true,
            }
        };
    };

    // get the shortened "local" readable type name(s)
    //
    // object either has no type: "untyped"
    // or has a single type
    // or has an array of types
    VisJsHelper.getShortType = function(j) {
        var typ = j.hasOwnProperty("@type") ? j["@type"] : "prefix#untyped";
        typ = typ.replaceAll(":", "#");   // for fuseki, change ":" separator to "#"

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

    // get the full type name
    VisJsHelper.getLongType = function(j) {
        var typ = j.hasOwnProperty("@type") ? j["@type"] : "untyped";
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
                        edgeList.push({
                            //id: p_id,
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
                        edgeList.push({
                            //id: p_id,
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
