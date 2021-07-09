#!/bin/bash
#
# must be run from current folder
#

# Stop if any command goes bad
set -e

if [ "$#" -ne 1 ] && [ "$#" -ne 2 ] && [ "$#" -ne 4 ]; then
    echo "Usage: ${BASH_SOURCE[0]} webapps_path [filter [search replace]]"
    exit 1
fi

# Get args
WEBAPPS="$1"
OPT_VARNAME_FILTER="${2:-^WEB_}"
OPT_VARNAME_SEARCH="$3"
OPT_VARNAME_REPLACE="$4"

if [ ! -d "${WEBAPPS}" ]; then
    echo "WEBAPPS is not a valid directory: ${WEBAPPS}"
    exit 1
fi

. ./.fun

# Try to get git SHA without crashing script
save=$-
set +e
GIT_SHA=`git rev-parse origin/master 2> /dev/null`
if [[ $save =~ e ]]; then
	set -e
else
	set +e
fi

# Create a BUILD id
if [[ -z "$GIT_SHA" ]]; then
	BUILD="built-"`hostname`"-"`date +%F-%H%M%S`
else
	BUILD="gitSHA-"${GIT_SHA}
fi
echo "Web build ID="${BUILD}

sed --in-place "s#%%BUILD%%#${BUILD}#g" "${WEBAPPS}/sparqlGraph/main-oss/sparqlGraph.html"

# define array of versioned files
declare -a VERSIONED=("sparqlGraph/main-oss/sparqlgraphconfigOss.js"
                      "sparqlGraph/main-oss/KDLEasyLoggerConfigOss.js"
                      "sparqlForm/main-oss/sparqlformconfig.js"
                      "sparqlForm/main-oss/KDLEasyLoggerConfig.js")

# replace versioned files
for v in "${VERSIONED[@]}"
do
    echo ${v}
    cp "./sparqlGraphWeb/${v}" "${WEBAPPS}/${v}"
    replace_vars_in_file "${WEBAPPS}/${v}" "${OPT_VARNAME_FILTER}" "${OPT_VARNAME_SEARCH}" "${OPT_VARNAME_REPLACE}"
done
