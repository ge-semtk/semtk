#!/bin/bash
#
# Starts SparqlGraph microservices.
#
# Usage: ./startSparqlGraphServices            to use default configuration files in src/main/resources
# Usage: ./startSparqlGraphServices CONFIG_DIR to use configuration files in CONFIG_DIR 

PORT_SPARQL_QUERY_SERVICE=12050
PORT_INGESTION_SERVICE=12091

if [ -z "$JAVA_HOME" ]; then
        >&2 echo No JAVA_HOME
        exit
fi

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LOGS=$SEMTK/logs
echo $SEMTK

# default config file locations
CONFIG_INGESTION_SERVICE="$SEMTK"/sparqlGraphIngestionService/src/main/resources/ingest.properties
# use different config files if given a config directory parameter
if [ $# -eq 1 ]; then
	echo USING CONFIG FILES IN $1 
	CONFIG_DIR=$1
    CONFIG_INGESTION_SERVICE="$CONFIG_DIR"/ingest.properties
else
	echo USING DEFAULT CONFIGS in src/main/resources/
fi

mkdir -p $LOGS

echo "=== START MICROSERVICES... ==="

"$JAVA_HOME"/bin/java -jar "$SEMTK"/sparqlQueryService/target/sparqlQueryService-*.jar --server.port=$PORT_SPARQL_QUERY_SERVICE > "$LOGS"/sparqlQueryService.log 2>&1 &

"$JAVA_HOME"/bin/java -jar "$SEMTK"/sparqlGraphIngestionService/target/sparqlGraphIngestionService-*.jar --spring.config.location="$CONFIG_INGESTION_SERVICE" --server.port=$PORT_INGESTION_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/sparqlGraphIngestionService.log 2>&1 &

echo "=== DONE ==="
