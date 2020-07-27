#!/bin/bash
#
# must be run from current folder
#

if [ -z "$WEBAPPS" ] ; then
	echo Error WEBAPPS is undefined
	exit 1
fi


# optional args
OPT_VARNAME_FILTER=$1
OPT_VARNAME_SEARCH=$2
OPT_VARNAME_REPLACE=$3

# define array of versioned files
declare -a VERSIONED=("sparqlGraph/main-oss/sparqlgraphconfigOss.js"
                      "sparqlGraph/main-oss/KDLEasyLoggerConfigOss.js" 
                      "sparqlForm/main-oss/sparqlformconfig.js"
                      "sparqlForm/main-oss/KDLEasyLoggerConfig.js")

# replace versioned files
for v in "${VERSIONED[@]}"
do
	cp "./sparqlGraphWeb/${v}" "${WEBAPPS}/${v}"
        replace_vars_in_file "${WEBAPPS}/${v}" "${OPT_VARNAME_FILTER}" "${OPT_VARNAME_SEARCH}" "${OPT_VARNAME_REPLACE}"
done
