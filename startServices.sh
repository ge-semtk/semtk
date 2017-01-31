#!/bin/bash
#
# Starts microservices, including the ones needed for SparqlGraph.
#

PORT_SPARQLGRAPH_STATUS_SERVICE=12051
PORT_SPARQLGRAPH_RESULTS_SERVICE=12052
PORT_HIVE_SERVICE=12055
PORT_ORACLE_SERVICE=
PORT_NODEGROUPSTORE_SERVICE=12056
PORT_ONTOLOGYINFO_SERVICE=12057

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

# start SPARQL query service, ingestion service
./startSparqlgraphServices.sh

"$JAVA_HOME"/bin/java -jar "$SEMTK"/ontologyInfoService/target/ontologyInfoService-1.1.0-SNAPSHOT.jar --spring.config.location="$SEMTK"/ontologyInfoService/src/main/resources/ontologyinfo.properties --server.port=$PORT_ONTOLOGYINFO_SERVICE > "$LOGS"/ontologyInfoService.log 2>&1 &

"$JAVA_HOME"/bin/java -jar "$SEMTK"/nodeGroupStoreService/target/nodeGroupStoreService-1.1.0-SNAPSHOT.jar --spring.config.location="$SEMTK"/nodeGroupStoreService/src/main/resources/store.properties --server.port=$PORT_NODEGROUPSTORE_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/nodeGroupStoreService.log 2>&1 &

"$JAVA_HOME"/bin/java -jar "$SEMTK"/sparqlGraphStatusService/target/sparqlGraphStatusService-1.1.0-SNAPSHOT.jar --spring.config.location="$SEMTK"/sparqlGraphStatusService/src/main/resources/status.properties --server.port=$PORT_SPARQLGRAPH_STATUS_SERVICE > "$LOGS"/sparqlGraphStatusService.log 2>&1 &

"$JAVA_HOME"/bin/java -jar "$SEMTK"/sparqlGraphResultsService/target/sparqlGraphResultsService-1.1.0-SNAPSHOT.jar --spring.config.location="$SEMTK"/sparqlGraphResultsService/src/main/resources/results.properties --server.port=$PORT_SPARQLGRAPH_RESULTS_SERVICE > "$LOGS"/sparqlGraphResultsService.log 2>&1 &

"$JAVA_HOME"/bin/java -jar "$SEMTK"/hiveService/target/hiveService-1.1.0-SNAPSHOT.jar --spring.config.location="$SEMTK"/hiveService/src/main/resources/hive.properties --server.port=$PORT_HIVE_SERVICE > "$LOGS"/hiveService.log 2>&1 &

#"$JAVA_HOME"/bin/java -jar "$SEMTK"/oracleService/target/oracleService-1.1.0-SNAPSHOT.jar --server.port=$PORT_ORACLE_SERVICE > "$LOGS"/oracleService.log 2>&1 &

echo "=== DONE ==="
