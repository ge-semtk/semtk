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


    VisJsHelper.addJsonLdObject = function(j, nodeList, edgeList) {

        console.log(JSON.stringify(j));

        // add the main node
        var shortType = j["@type"].split("#")[1];
        var groupVal = j["@type"];
        if (shortType.indexOf("XMLSchema") > -1) {
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
                        // add edge to data
                        nodeList.push({
                            id : o["@value"],
                            label: o["@value"],
                            title: o["@type"],
                            group: "data"
                        });
                        edgeList.push({
                            //id: p_id,
                            from: j["@id"],
                            to: o["@value"],
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
