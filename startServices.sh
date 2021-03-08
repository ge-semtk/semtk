#!/bin/bash
#
# Copyright 2018 General Electric Company
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
# Optional argument:  full path of dir with alternate versions of:
#                       .env, .fun, logs/ ENV_OVERRIDE


# SEMTK = directory holding this script
# try windows style pwd -W first, then default to unix
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd -W 2>/dev/null)"
if [ -z $SEMTK ]
then
        SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi
echo SEMTK is $SEMTK

# Handle $1 optional arg of alternate semtk dir that contains
#    ENV_OVERRIDE 
if [ $# -eq 1 ]; then
	LOGS=${1}/logs  
	cp ./.env ${1}
	cp ./.fun ${1}
	pushd ${1}; . .env; popd

elif [ $# -eq 0 ]; then
	LOGS=${SEMTK}/logs
	pushd ${SEMTK}; . .env; popd

else 
	echo Usage: startServices.sh [alt_env_dir]
fi

# exit if no JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
        >&2 echo No JAVA_HOME
        exit 1
fi

# exit if services are still running
NUM_SERVICES_RUNNING=`ps aux | grep jar | grep $SEMTK | grep Service | wc -l`
if [ $NUM_SERVICES_RUNNING -gt 0 ]; then
        >&2 echo "$NUM_SERVICES_RUNNING services are running in $SEMTK, cannot restart"
        exit 1
else
        >&2 echo "Confirmed no services running in $SEMTK"
fi

# logs/
mkdir -p $LOGS

#
# "Legacy" startServices will not config webapps
#
if [ -n "${WEBAPPS}" ]; then
	./configWebapps.sh
fi


echo "=== START MICROSERVICES... ==="

PID_ARRAY=()
PROC_ARRAY=()
PORT_ARRAY=()

if [[ "$ENABLED_SERVICES" == *ontologyInfoService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/ontologyInfoService/target/ontologyInfoService-*.jar > "$LOGS"/ontologyInfoService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("ontologyInfoService");
	PORT_ARRAY+=($PORT_ONTOLOGYINFO_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *nodeGroupStoreService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupStoreService/target/nodeGroupStoreService-*.jar > "$LOGS"/nodeGroupStoreService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("nodeGroupStoreService");
	PORT_ARRAY+=($PORT_NODEGROUPSTORE_SERVICE)
	
fi

if [[ "$ENABLED_SERVICES" == *sparqlGraphStatusService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphStatusService/target/sparqlGraphStatusService-*.jar > "$LOGS"/sparqlGraphStatusService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("sparqlGraphStatusService");
	PORT_ARRAY+=($PORT_SPARQLGRAPH_STATUS_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *sparqlGraphResultsService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS_LARGE_MEMORY -jar "$SEMTK"/sparqlGraphResultsService/target/sparqlGraphResultsService-*.jar > "$LOGS"/sparqlGraphResultsService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("sparqlGraphResultsService");
	PORT_ARRAY+=($PORT_SPARQLGRAPH_RESULTS_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *hiveService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/hiveService/target/hiveService-*.jar > "$LOGS"/hiveService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("hiveService");
	PORT_ARRAY+=($PORT_HIVE_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *sparqlExtDispatchService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS_LARGE_MEMORY -Dloader.path="${LOCATION_ADDITIONAL_DISPATCHER_JARS}" -jar "$SEMTK"/sparqlExtDispatchService/target/sparqlExtDispatchService-*.jar > "$LOGS"/sparqlExtDispatchService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("sparqlExtDispatchService");
	PORT_ARRAY+=($PORT_DISPATCH_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *nodeGroupExecutionService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupExecutionService/target/nodeGroupExecutionService-*.jar > "$LOGS"/nodeGroupExecutionService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("nodeGroupExecutionService");
	PORT_ARRAY+=($PORT_NODEGROUPEXECUTION_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *sparqlQueryService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlQueryService/target/sparqlQueryService-*.jar > "$LOGS"/sparqlQueryService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("sparqlQueryService");
	PORT_ARRAY+=($PORT_SPARQL_QUERY_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *sparqlGraphIngestionService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/sparqlGraphIngestionService/target/sparqlGraphIngestionService-*.jar > "$LOGS"/sparqlGraphIngestionService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("sparqlGraphIngestionService");
	PORT_ARRAY+=($PORT_INGESTION_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *nodeGroupService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/nodeGroupService/target/nodeGroupService-*.jar > "$LOGS"/nodeGroupService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("nodeGroupService");
	PORT_ARRAY+=($PORT_NODEGROUP_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *fdcSampleService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/fdcSampleService/target/fdcSampleService-*.jar > "$LOGS"/fdcSampleService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("fdcSampleService");
	PORT_ARRAY+=($PORT_FDCSAMPLE_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *fdcCacheService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/fdcCacheService/target/fdcCacheService-*.jar > "$LOGS"/fdcCacheService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("fdcCacheService");
	PORT_ARRAY+=($PORT_FDCCACHE_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *edcQueryGenerationService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/edcQueryGenerationService/target/edcQueryGenerationService-*.jar > "$LOGS"/edcQueryGenerationService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("edcQueryGenerationService");
	PORT_ARRAY+=($PORT_EDCQUERYGEN_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *athenaService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS_LARGE_MEMORY -jar "$SEMTK"/athenaService/target/athenaService-*.jar > "$LOGS"/athenaService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("athenaService");
	PORT_ARRAY+=($PORT_ATHENA_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *arangoDbService* ]]; then
	"$JAVA_HOME"/bin/java -jar "$SEMTK"/arangoDbService/target/arangoDbService-*.jar > "$LOGS"/arangoDbService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("arangoDbService");
	PORT_ARRAY+=($PORT_ARANGODB_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *utilityService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/utilityService/target/utilityService-*.jar > "$LOGS"/utilityService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("utilityService");
	PORT_ARRAY+=($PORT_UTILITY_SERVICE)
fi

if [[ "$ENABLED_SERVICES" == *fileStagingService* ]]; then
	"$JAVA_HOME"/bin/java $JVM_OPTIONS -jar "$SEMTK"/fileStagingService/target/fileStagingService-*.jar > "$LOGS"/fileStagingService.log 2>&1 &
	PID_ARRAY+=($!)
	PROC_ARRAY+=("fileStagingService");
	PORT_ARRAY+=($PORT_FILESTAGING_SERVICE)
fi

#
# wait for services
#
MAX_SEC=300


# protocol for ping
if [ "$SSL_ENABLED" == "false" ]; then
    PROTOCOL="http"
else
	PROTOCOL="https"
fi				 

echo Using no_proxy: $no_proxy

# check for each service				 
for port in "${PORT_ARRAY[@]}"; do
   while ! curl --insecure --noproxy $no_proxy -X POST ${PROTOCOL}://${HOST_NAME}:${port}/serviceInfo/ping 2>>/dev/null | grep -q yes ; do

	echo waiting for service at ${PROTOCOL}://${HOST_NAME}:${port}
        if (($SECONDS > $MAX_SEC)) ; then
        	echo ERROR: Took to longer than $MAX_SEC seconds to start services
        	exit 1
        fi
        sleep 3

	# check that no processes have died yet
	for i in "${!PID_ARRAY[@]}"; do
		if (! (ps -p ${PID_ARRAY[$i]} >> /dev/null)) ; then
			echo ERROR: Process ${PID_ARRAY[$i]} ${PROC_ARRAY[$i]} has died
			exit 1
		fi
	done
   done
   echo service at ${PROTOCOL}://${HOST_NAME}:${port} is up
done
echo "=== DONE ==="
exit 0
