#!/bin/bash

# This script is the entrypoint of the kdl-sparql Docker image
# It is reposnsible for configuring the web application and starting it
# It relies on environment variables that should be defined before this script executes

# Configure app
echo ""
echo "Configuring sparqlGraph UI ..."
INGEST_SEARCH=http://localhost:12091/ingestion/
QUERY_SEARCH=http://localhost:12050/sparqlQueryService/
STATUS_SEARCH=http://localhost:12051/status/
RESULTS_SEARCH=http://localhost:12052/results/
DISPATCHER_SEARCH=http://localhost:12053/dispatcher/
HIVE_SEARCH=http://localhost:12055/hiveService/
NGSTORE_SEARCH=http://localhost:12056/nodeGroupStore/
OINFO_SEARCH=http://localhost:12057/ontologyinfo/
NGEXEC_SEARCH=http://localhost:12058/nodeGroupExecution/
NG_SEARCH=http://localhost:12059/nodeGroup/
CONNECTION_SEARCH=http://localhost:2420
FILE_PATH=/usr/local/tomcat/webapps/sparqlGraph/main-oss/sparqlgraphconfigOss.js
FILE2_PATH=/usr/local/tomcat/webapps/sparqlGraph/main-oss/demoNodegroup.json
sed --in-place "s#${INGEST_SEARCH}#${INGEST_URL}#g" "${FILE_PATH}"
sed --in-place "s#${QUERY_SEARCH}#${QUERY_URL}#g" "${FILE_PATH}"
sed --in-place "s#${STATUS_SEARCH}#${STATUS_URL}#g" "${FILE_PATH}"
sed --in-place "s#${RESULTS_SEARCH}#${RESULTS_URL}#g" "${FILE_PATH}"
sed --in-place "s#${DISPATCHER_SEARCH}#${DISPATCHER_URL}#g" "${FILE_PATH}"
sed --in-place "s#${HIVE_SEARCH}#${HIVE_URL}#g" "${FILE_PATH}"
sed --in-place "s#${NGSTORE_SEARCH}#${NGSTORE_URL}#g" "${FILE_PATH}"
sed --in-place "s#${OINFO_SEARCH}#${OINFO_URL}#g" "${FILE_PATH}"
sed --in-place "s#${NGEXEC_SEARCH}#${NGEXEC_URL}#g" "${FILE_PATH}"
sed --in-place "s#${NG_SEARCH}#${NG_URL}#g" "${FILE_PATH}"
sed --in-place "s#${CONNECTION_SEARCH}#${triplestoreServerAndPort}#g" "${FILE2_PATH}"
cat /usr/local/tomcat/webapps/sparqlGraph/main-oss/sparqlgraphconfigOss.js

# Start tomcat
echo ""
echo "Starting Tomcat ..."
/usr/local/tomcat/bin/catalina.sh run

