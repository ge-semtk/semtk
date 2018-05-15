#!/bin/bash
#
# Copyright 2016 General Electric Company
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
#
# Starts microservices, including the ones needed for SparqlGraph.
#
# Usage: ./startServices                                to use default configuration files in src/main/resources
# Usage: ./startServices CONFIG_DIR                     to use configuration files in CONFIG_DIR 
# Usage: ./startServices CONFIG_DIR DISPATCHER_JAR_DIR  to add additional dispatcher jars


# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LOGS=$SEMTK/logs
echo $SEMTK

source $SEMTK/.env

# JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
        >&2 echo No JAVA_HOME
        exit 1
fi

# default config file locations 
CONFIG_NODEGROUPSTORE_SERVICE="$SEMTK"/nodeGroupStoreService/src/main/resources/store.properties 
CONFIG_SPARQLGRAPH_RESULTS_SERVICE="$SEMTK"/sparqlGraphResultsService/src/main/resources/results.properties 
CONFIG_INGESTION_SERVICE="$SEMTK"/sparqlGraphIngestionService/src/main/resources/ingest.properties

# use different config files if given a config directory parameter
if [ $# -gt 0 ]; then
	CONFIG_DIR=$1
	echo USING CONFIG FILES IN "$CONFIG_DIR"
    CONFIG_NODEGROUPSTORE_SERVICE="$CONFIG_DIR"/store.properties
    CONFIG_SPARQLGRAPH_RESULTS_SERVICE="$CONFIG_DIR"/results.properties
    CONFIG_INGESTION_SERVICE="$CONFIG_DIR"/ingest.properties
else
	echo USING DEFAULT CONFIGS in src/main/resources/
fi

if [ $# -gt 1 ]; then
	echo ADDING DISPATCHER JARS IN $2
	LOCATION_ADDITIONAL_DISPATCHER_JARS=$2
else
	LOCATION_ADDITIONAL_DISPATCHER_JARS=""
fi

mkdir -p $LOGS

echo "=== START MICROSERVICES... ==="

# start SPARQL query service, ingestion service

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/ontologyInfoService/target/ontologyInfoService-*.jar > "$LOGS"/ontologyInfoService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupStoreService/target/nodeGroupStoreService-*.jar --spring.config.location="$CONFIG_NODEGROUPSTORE_SERVICE" --server.port=$PORT_NODEGROUPSTORE_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/nodeGroupStoreService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphStatusService/target/sparqlGraphStatusService-*.jar > "$LOGS"/sparqlGraphStatusService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphResultsService/target/sparqlGraphResultsService-*.jar --spring.config.location="$CONFIG_SPARQLGRAPH_RESULTS_SERVICE" --server.port=$PORT_SPARQLGRAPH_RESULTS_SERVICE --multipart.maxFileSize=1000Mb --multipart.maxRequestSize=1000Mb > "$LOGS"/sparqlGraphResultsService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/hiveService/target/hiveService-*.jar > "$LOGS"/hiveService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -Dloader.path="$LOCATION_ADDITIONAL_DISPATCHER_JARS" -jar "$SEMTK"/sparqlExtDispatchService/target/sparqlExtDispatchService-*.jar > "$LOGS"/sparqlExtDispatchService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupExecutionService/target/nodeGroupExecutionService-*.jar > "$LOGS"/nodeGroupExecutionService.log 2>&1 &

#"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/oracleService/target/oracleService-*.jar --server.port=$PORT_ORACLE_SERVICE > "$LOGS"/oracleService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlQueryService/target/sparqlQueryService-*.jar --server.port=$PORT_SPARQL_QUERY_SERVICE > "$LOGS"/sparqlQueryService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphIngestionService/target/sparqlGraphIngestionService-*.jar --spring.config.location="$CONFIG_INGESTION_SERVICE" --server.port=$PORT_INGESTION_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/sparqlGraphIngestionService.log 2>&1 &

"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupService/target/nodeGroupService-*.jar --server.port=$PORT_NODEGROUP_SERVICE --multipart.maxFileSize=1000Mb > "$LOGS"/nodeGroupService.log 2>&1 &

#
# wait for services
#
MAX_SEC=300
declare -a PORTS=($PORT_SPARQLGRAPH_STATUS_SERVICE
                  $PORT_SPARQLGRAPH_RESULTS_SERVICE
                  $PORT_DISPATCH_SERVICE
                  $PORT_HIVE_SERVICE
                  $PORT_NODEGROUPSTORE_SERVICE
                  $PORT_ONTOLOGYINFO_SERVICE
                  $PORT_NODEGROUPEXECUTION_SERVICE
                  $PORT_SPARQL_QUERY_SERVICE
                  $PORT_INGESTION_SERVICE
                  $PORT_NODEGROUP_SERVICE
                 )

for port in "${PORTS[@]}"; do
   while !  curl -X POST http://localhost:${port}/serviceInfo/ping 2>>/dev/null | grep -q yes ; do
        echo waiting for service on port $port
        if (($SECONDS > $MAX_SEC)) ; then
        	echo ERROR: Took to longer than $MAX_SEC seconds to start services
        	exit 1
        fi
        sleep 3
   done
   echo service on port $port is up
done
echo "=== DONE ==="
exit 0
