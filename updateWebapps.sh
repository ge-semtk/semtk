#!/bin/bash
#
#  Moves html, js, css, etc onto the apache web server
#

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
                echo cp $WEBAPPS/$v $TMP
                cp $WEBAPPS/$v $TMP
        fi
done

# get list of dirs to move to webapps

COPYDIRS=( "iidx-oss"
           "semtk-api-doc"
           "sparqlForm/js"
           "sparqlForm/main-oss"
           "sparqlGraph/main-oss"
         )
cd $SEMTK_OSS

# process each dir
for DIR in "${COPYDIRS[@]}"
do
        DEST_DIR=$(dirname ${WEBAPPS}/${DIR})
        
        # Wipe out and replace other known dirs
        echo rm -rf $WEBAPPS/$DIR
             rm -rf $WEBAPPS/$DIR

        echo cp -r $SPARQLGRAPHWEB/$DIR $DEST_DIR
             cp -r $SPARQLGRAPHWEB/$DIR $DEST_DIR
done

# --- special cases ---
# Allow other files to remain in ROOT
echo mkdir -p $SPARQLGRAPHWEB/ROOT
     mkdir -p $SPARQLGRAPHWEB/ROOT
echo cp -r $SPARQLGRAPHWEB/ROOT/* $WEBAPPS/ROOT
     cp -r $SPARQLGRAPHWEB/ROOT/* $WEBAPPS/ROOT

# Copy over html files from sparqlForm & sparqlGraph
echo cp $SPARQLGRAPHWEB/sparqlForm/*.html $WEBAPPS/sparqlForm
     cp $SPARQLGRAPHWEB/sparqlForm/*.html $WEBAPPS/sparqlForm
echo cp $SPARQLGRAPHWEB/sparqlGraph/*.html $WEBAPPS/sparqlGraph
     cp $SPARQLGRAPHWEB/sparqlGraph/*.html $WEBAPPS/sparqlGraph
        
# replace versioned files
for v in "${VERSIONED[@]}"
do
        SAVED=$TMP/${v##*/}
        CURRENT=$WEBAPPS/$v

        if [ ! -e $SAVED ]; then
                echo WARNING: file needs to be modified for local configuration: $CURRENT
        else
                CURRENT_VERSION="($(grep VERSION $CURRENT))"
                SAVED_VERSION="($(grep VERSION $SAVED))"

                if [ "$CURRENT_VERSION" == "$SAVED_VERSION" ]; then 
                        echo cp $SAVED $CURRENT
                        cp $SAVED $CURRENT
                else
                        echo WARNING: these files need to be manually merged to keep local configuration:
                        echo "        $SAVED"
                        echo "        $CURRENT"
                        exit 1
                fi
        fi
done
exit 0
                           