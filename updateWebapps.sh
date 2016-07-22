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

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SPARQLGRAPHWEB=$SEMTK/sparqlGraphWeb

# make tmp directory
TMP=$SEMTK/tmp
rm -rf $TMP
mkdir $TMP

# maddening case-fixing
if [ -e $SPARQLGRAPHWEB/sparqlForm/main-OSS ]; then
        echo mv $SPARQLGRAPHWEB/sparqlForm/main-OSS $SPARQLGRAPHWEB/sparqlForm/main-oss
        mv $SPARQLGRAPHWEB/sparqlForm/main-OSS $SPARQLGRAPHWEB/sparqlForm/main-oss
fi
if [ -e $SPARQLGRAPHWEB/sparqlGraph/main-OSS ]; then
        echo mv $SPARQLGRAPHWEB/sparqlGraph/main-OSS $SPARQLGRAPHWEB/sparqlGraph/main-oss
        mv $SPARQLGRAPHWEB/sparqlGraph/main-OSS $SPARQLGRAPHWEB/sparqlGraph/main-oss
fi

# define array of versioned files
declare -a VERSIONED=("sparqlGraph/main-oss/sparqlgraphconfigOss.js" "sparqlForm/main-oss/sparqlformconfig.js")

# copy versioned files to $TMP
for v in "${VERSIONED[@]}"
do
        if [ -e $WEBAPPS/$v ]; then
                echo cp $WEBAPPS/$v $TMP
                cp $WEBAPPS/$v $TMP
        fi
done
# get list of dirs to move to webapps
cd $SPARQLGRAPHWEB
COPYDIRS=($(ls -d */))
cd $SEMTK

# process each array
for d in "${COPYDIRS[@]}"
do
        DIR=${d%?}   # remove trailing /
        if [ $DIR == "ROOT" ]; then
                # Allow other files to remain in ROOT
                mkdir -p $SPARQLGRAPHWEB/$DIR
                echo cp -r $SPARQLGRAPHWEB/$DIR/* $WEBAPPS/$DIR
                cp -r $SPARQLGRAPHWEB/$DIR/* $WEBAPPS/$DIR
        else
                # Wipe out and replace other known dirs
                echo rm -rf $WEBAPPS/$DIR
                rm -rf $WEBAPPS/$DIR

                echo cp -r $SPARQLGRAPHWEB/$DIR $WEBAPPS/
                cp -r $SPARQLGRAPHWEB/$DIR $WEBAPPS/
        fi
done

# replace versioned files
for v in "${VERSIONED[@]}"
do
        SAVED=$TMP/${v##*/}
        CURRENT=$WEBAPPS/$v

        if [ ! -e $SAVED ]; then
                echo WARNING: file needs to be modified for local configuration: $CURRENT
        else
                CURRENT_VERSION=($(grep VERSION $CURRENT))
                SAVED_VERSION=($(grep VERSION $SAVED))

                if [ "$CURRENT_VERSION" == "$SAVED_VERSION" ]; then
                        echo cp $CURRENT $SAVED
                        cp $CURRENT $SAVED
                else
                        echo WARNING: these files need to be manually merged to keep local configuration:
                        echo "        $SAVED"
                        echo "        $CURRENT"
                fi
        fi
done
                           