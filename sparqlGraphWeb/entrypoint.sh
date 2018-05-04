#!/bin/bash

# This script is the entrypoint of the kdl-sparql Docker image
# It is reposnsible for configuring the web application and starting it
# It relies on environment variables that should be defined before this script executes

# Configure app
echo ""
echo "Configuring sparqlGraph UI ..."

FILE_PATH=/usr/local/tomcat/webapps/sparqlGraph/main-oss/sparqlgraphconfigOss.js

replace_vars_in_file ${FILE_PATH} "^WEB_"

# Start tomcat
echo ""
echo "Starting Tomcat ..."
/usr/local/tomcat/bin/catalina.sh run

