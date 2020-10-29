#!/bin/bash
#
#  Moves html, js, css, etc onto the apache web server
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

# SCRIPT_HOME = directory holding this script
SCRIPT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd -P)"
SG_WEB_OSS="${SCRIPT_HOME}/sparqlGraphWeb"

# get environment variables
pushd "${SCRIPT_HOME}"
. ./.env
popd

# make sure these exist in webapps
# or else the COPYDIRS might fail in weird ways
CREATEDIRS=(
           "sparqlGraph"
           "sparqlForm"
         )
for DIR in "${CREATEDIRS[@]}"
do
    mkdir -p "${WEBAPPS}/${DIR}"
done

# get list of dirs to copy to webapps
COPYDIRS=( "iidx-oss"
           "sparqlForm/main-oss"
           "sparqlGraph/css"
           "sparqlGraph/dynatree-1.2.5"
           "sparqlGraph/images"
           "sparqlGraph/jquery"
           "sparqlGraph/js"
           "sparqlGraph/main-oss"
           "vis-4.21.0"
         )
cd "${SCRIPT_HOME}"

# process each dir
for DIR in "${COPYDIRS[@]}"
do
        DEST_DIR=$(dirname "${WEBAPPS}/${DIR}")

        # Wipe out and replace other known dirs
        rm -rf "${WEBAPPS:?}/${DIR}"
        cp -r "${SG_WEB_OSS}/${DIR}" "${DEST_DIR}"
done

# --- special cases ---

# Allow other files to remain in ROOT
mkdir -p "${SG_WEB_OSS}"/ROOT
cp -r "${SG_WEB_OSS}"/ROOT/* "${WEBAPPS}"/ROOT

# Copy over html files from sparqlForm & sparqlGraph
cp "${SG_WEB_OSS}"/sparqlForm/*.html "${WEBAPPS}"/sparqlForm
cp "${SG_WEB_OSS}"/sparqlGraph/*.html "${WEBAPPS}"/sparqlGraph

./configWebapps.sh "${WEBAPPS}" "${OPT_VARNAME_FILTER}" "${OPT_VARNAME_SEARCH}" "${OPT_VARNAME_REPLACE}"

echo "${BASH_SOURCE[0]} done"
