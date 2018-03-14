#!/bin/bash
#
#  Moves html, js, css, etc onto the apache web server
#

# stop if anything goes bad
set -e

if [ "$#" -ne 1 ]; then
    echo "Usage: updateWebapps.sh webapps_path"
    exit 1
fi

# Get webapps
WEBAPPS=$1

if [ ! -d ${WEBAPPS} ]; then
    echo "Usage: updateWebapps.sh webapps_path"
    exit 1
fi

# SEMTK_OSS = directory holding this script
SEMTK_OSS="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SG_WEB_OSS=${SEMTK_OSS}/sparqlGraphWeb

# make tmp directory
TMP=${SEMTK_OSS}/tmp
rm -rf ${TMP}
mkdir ${TMP}

# define array of versioned files
declare -a VERSIONED=("sparqlGraph/main-oss/sparqlgraphconfigOss.js"
                      "sparqlGraph/main-oss/KDLEasyLoggerConfigOss.js" 
                      "sparqlForm/main-oss/sparqlformconfig.js"
                      "sparqlForm/main-oss/KDLEasyLoggerConfig.js")

# copy versioned files to ${TMP}
for v in "${VERSIONED[@]}"
do
        if [ -e ${WEBAPPS}/${v} ]; then
                set -x
                cp ${WEBAPPS}/${v} ${TMP}
                set +x
        fi
done

# make sure these exist in webapps
# or else the COPYDIRS might fail in weird ways
CREATEDIRS=( 
           "sparqlGraph"
           "sparqlForm"
         )
for DIR in "${CREATEDIRS[@]}"
do
	mkdir -p ${WEBAPPS}/${DIR}
done

# get list of dirs to copy to webapps
COPYDIRS=( "iidx-oss"
           "semtk-api-doc"
           "sparqlForm/main-oss"
           "sparqlGraph/css"
           "sparqlGraph/dynatree-1.2.5"
           "sparqlGraph/images"
           "sparqlGraph/jquery"
           "sparqlGraph/js"
           "sparqlGraph/main-oss"
         )
cd ${SEMTK_OSS}

# process each dir
for DIR in "${COPYDIRS[@]}"
do
        DEST_DIR=$(dirname ${WEBAPPS}/${DIR})
        
        # Wipe out and replace other known dirs
        set -x
        
        rm -rf ${WEBAPPS}/${DIR}
		cp -r ${SG_WEB_OSS}/${DIR} ${DEST_DIR}
		
		set +x        
done

# --- special cases ---
set -x

# Allow other files to remain in ROOT
mkdir -p ${SG_WEB_OSS}/ROOT
cp -r ${SG_WEB_OSS}/ROOT/* ${WEBAPPS}/ROOT

# Copy over html files from sparqlForm & sparqlGraph
cp ${SG_WEB_OSS}/sparqlForm/*.html ${WEBAPPS}/sparqlForm
cp ${SG_WEB_OSS}/sparqlGraph/*.html ${WEBAPPS}/sparqlGraph

set +x
   
WARNINGS=0
# replace versioned files
for v in "${VERSIONED[@]}"
do
        SAVED=${TMP}/${v##*/}
        CURRENT=${WEBAPPS}/${v}

        if [ ! -e ${SAVED} ]; then
                echo WARNING: file needs to be modified for local configuration: ${CURRENT}
                WARNINGS=1
        else
				set +e
				CURRENT_VERSION="$(grep VERSION ${CURRENT})"
                SAVED_VERSION="$(grep VERSION ${SAVED})"
				set -e

				if [ -z "${CURRENT_VERSION}" ]; then
					echo "file is missing expected VERSION: ${CURRENT}"
				fi
				if [ -z "${SAVED_VERSION}" ]; then
					echo "file is missing expected VERSION: ${SAVED}"
				fi

				echo versions ${CURRENT_VERSION}  ${SAVED_VERSION}
				if [ "${CURRENT_VERSION}" == "${SAVED_VERSION}" ]; then
					set -x
					cp ${SAVED} ${CURRENT}
					set +x
				else
					echo "WARNING: Git pull has overwritten web config:"
					echo ">   deployed version:   ${CURRENT}"
					echo ">   prev version saved: ${SAVED}"
				 	echo ">  "
					echo ">   You should manually merge into ${CURRENT} before the next build"
					echo ">   As next build will succeed regardless."
					WARNINGS=1
				fi
        fi
        
        set -x
        chmod 666 ${CURRENT}
        set +x
done

if [ "${WARNINGS}" -ne "0" ]; then
	set -x
	exit 1
	set +x
fi

echo ==== updateWebapps.sh SUCCESS ====


                