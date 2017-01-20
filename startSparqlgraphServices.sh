#!/bin/bash
#
# Starts SparqlGraph microservices.
#

PORT_SPARQL_QUERY_SERVICE=12050
PORT_SPARQL_ONTOLOGY_INFO_SERVICE=12057
PORT_SPARQL_NODE_GROUP_SERVICE=12058
PORT_INGESTION_SERVICE=12091

if [ -z ${JAVA_HOME} ]; then
        >&2 echo No JAVA_HOME
        exit
fi

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LOGS=$SEMTK/logs
echo $SEMTK

mkdir -p $LOGS

echo "=== START MICROSERVICES... ==="
$JAVA_HOME/bin/java -jar $SEMTK/sparqlQueryService/target/sparqlQueryService-1.1.0-SNAPSHOT.jar --server.port=$PORT_SPARQL_QUERY_SERVICE > $LOGS/sparqlQueryService.log 2>&1 &

$JAVA_HOME/bin/java -jar $SEMTK/sparqlGraphIngestionService/target/sparqlGraphIngestionService-1.1.0-SNAPSHOT.jar --spring.config.location=$SEMTK/sparqlGraphIngestionService/src/main/resources/ingest.properties --server.port=$PORT_INGESTION_SERVICE --multipart.maxFileSize=1000Mb > $LOGS/sparqlGraphIngestionService.log 2>&1 &

$JAVA_HOME/bin/java -jar $SEMTK/ontologyInfoService/target/ontologyInfoService-1.1.0-SNAPSHOT.jar --spring.config.location=$SEMTK/sparqlGraphIngestionService/src/main/resources/ingest.properties --server.port=$PORT_ONTOLOGY_INFO_SERVICE --multipart.maxFileSize=1000Mb > $LOGS/sparqlGraphIngestionService.log 2>&1 &

$JAVA_HOME/bin/java -jar $SEMTK/nodeGroupStoreService/target/nodeGroupStoreService-1.1.0-SNAPSHOT.jar --spring.config.location=$SEMTK/sparqlGraphIngestionService/src/main/resources/ingest.properties --server.port=$PORT_NODE_GROUP_SERVICE --multipart.maxFileSize=1000Mb > $LOGS/sparqlGraphIngestionService.log 2>&1 &

echo "=== DONE ==="
