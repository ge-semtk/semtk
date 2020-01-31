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

    // object either has no type: "untyped"
    // or has a single type
    // or has an array of types
    VisJsHelper.getShortType = function(j) {
        var typ = j.hasOwnProperty("@type") ? j["@type"] : "prefix#untyped";
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

    VisJsHelper.addJsonLdObject = function(j, nodeList, edgeList) {

        console.log(JSON.stringify(j));

        // add the main node
        var shortType = VisJsHelper.getShortType(j);
        var groupVal = VisJsHelper.getLongType(j);
        if (groupVal.indexOf("XMLSchema") > -1) {
            groupVal = "data";
        }
        nodeList.push({
            id : j["@id"],
            label: shortType,
            title: j["@id"],
            group: groupVal
        });

        // loop through properties
        for (var key in j) {
            if (key[0] != "@") {
                var predName = key.split("#")[1];
                // make predicates an array
                var objects;
                if (Array.isArray(j[key])) {
                    objects = j[key];
                } else {
                    objects = [ j[key] ];
                }

                // for each object
                for (var i=0; i < objects.length; i++ ) {
                    var o = objects[i];
                    if (o.hasOwnProperty("@id")) {
                        // add edge to a jsonLd object
                        edgeList.push({
                            //id: p_id,
                            from: j["@id"],
                            to: o["@id"],
                            label: predName,
                            arrows: 'to',
                            color: {inherit: false},
                            group: key
                        });
                    } else {
                        // sometimes virtuoso returns 35 instead of { @value: 35, @type: integer }
                        var val = o.hasOwnProperty("@value") ? o["@value"] : o.toString();
                        var typ = VisJsHelper.getLongType(o);
                        // add edge to data
                        nodeList.push({
                            id : val,
                            label: val,
                            title: typ,
                            group: "data"
                        });
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
