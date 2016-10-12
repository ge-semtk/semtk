#!/bin/bash

# This script is the entrypoint of the kdl-sparql Docker image
# It is reposnsible for configuring the web application and starting it
# It relies on environment variables that should be defined before this script executes

# Configure app
echo ""
echo "Configuring sparqlGraph UI ..."
INGEST_SEARCH=http://vesuvius37.crd.ge.com:9191/ingestion/
QUERY_SEARCH=http://vesuvius37.crd.ge.com:12050/sparqlQueryService/
FILE_PATH=/usr/local/tomcat/webapps/sparqlGraph/main-oss/sparqlgraphconfigOss.js
sed --in-place "s#${INGEST_SEARCH}#${INGEST_URL}#g" "${FILE_PATH}"
sed --in-place "s#${QUERY_SEARCH}#${QUERY_URL}#g" "${FILE_PATH}"
cat /usr/local/tomcat/webapps/sparqlGraph/main-oss/sparqlgraphconfigOss.js

# Start tomcat
echo ""
echo "Starting Tomcat ..."
/usr/local/tomcat/bin/catalina.sh run

