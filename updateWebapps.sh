#!/bin/bash
#
#  Moves html, js, css, etc onto the apache web server
#

# stop if anything goes bad
set -o erronexit

if [ "$#" -ne 1 ]; then
    echo "Usage: updateWebapps.sh webapps_path"
fi

# Get webapps
WEBAPPS=$1

if [ ! -d $WEBAPPS ]; then
    echo "Usage: updateWebapps.sh webapps_path"
fi

# SEMTK_OSS = directory holding this script
SEMTK_OSS="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SPARQLGRAPHWEB=$SEMTK_OSS/sparqlGraphWeb

# make tmp directory
TMP=$SEMTK_OSS/tmp
rm -rf $TMP
mkdir $TMP

# define array of versioned files
declare -a VERSIONED=("sparqlGraph/main-oss/sparqlgraphconfigOss.js"
                      "sparqlGraph/main-oss/KDLEasyLoggerConfigOss.js" 
                      "sparqlForm/main-oss/sparqlformconfig.js"
                      "sparqlForm/main-oss/KDLEasyLoggerConfig.js")

# copy versioned files to $TMP
for v in "${VERSIONED[@]}"
do
        if [ -e $WEBAPPS/$v ]; then
                set -o xtrace
                cp $WEBAPPS/$v $TMP
                set +o xtrace
        fi
done

# get list of dirs to move to webapps
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
cd $SEMTK_OSS

# process each dir
for DIR in "${COPYDIRS[@]}"
do
        DEST_DIR=$(dirname ${WEBAPPS}/${DIR})
        
        # Wipe out and replace other known dirs
        set -o xtrace
        
        rm -rf $WEBAPPS/$DIR
		cp -r $SG_WEB_GE/$DIR $DEST_DIR
		
		set +o xtrace        
done

# --- special cases ---
set -o xtrace

# Allow other files to remain in ROOT
mkdir -p $SPARQLGRAPHWEB/ROOT
cp -r $SPARQLGRAPHWEB/ROOT/* $WEBAPPS/ROOT

# Copy over html files from sparqlForm & sparqlGraph
cp $SPARQLGRAPHWEB/sparqlForm/*.html $WEBAPPS/sparqlForm
cp $SPARQLGRAPHWEB/sparqlGraph/*.html $WEBAPPS/sparqlGraph

set +o xtrace
   
WARNINGS=0
# replace versioned files
for v in "${VERSIONED[@]}"
do
        SAVED=$TMP/${v##*/}
        CURRENT=$WEBAPPS/$v

        if [ ! -e $SAVED ]; then
                echo WARNING: file needs to be modified for local configuration: $CURRENT
                WARNINGS=1
        else	
        		echo grepping version info from $CURRENT and $SAVED
                CURRENT_VERSION=($(grep VERSION $CURRENT))
                SAVED_VERSION=($(grep VERSION $SAVED))

                if [ "$CURRENT_VERSION" == "$SAVED_VERSION" ]; then
                        set -o xtrace
                        cp $SAVED $CURRENT
                        set +o xtrace
                else
                        echo WARNING: these files need to be manually merged to keep local configuration:
                        echo "        $SAVED"
                        echo "        $CURRENT"
                        WARNINGS=1
                fi
        fi
        
        set -o xtrace
        chmod 666 $CURRENT
        set +o xtrace
done

if [ "$WARNINGS" -ne "0" ]; then
	exit 1
fi


                