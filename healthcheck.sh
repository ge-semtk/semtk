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
# Optional argument:  full path of dir with alternate versions of:
#                       .env, .fun, logs/ ENV_OVERRIDE


# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
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

#
# wait for services
#
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
# protocol for ping
if [ "$SSL_ENABLED" == "false" ]; then
    PROTOCOL="http"
else
	PROTOCOL="https"
fi				 

echo Using no_proxy: $no_proxy

# check for each service				 
for port in "${PORTS[@]}"; do
   echo ${PROTOCOL}://${HOST_NAME}:${port}/serviceInfo/ping
   if ! curl --insecure --noproxy $no_proxy -X POST ${PROTOCOL}://${HOST_NAME}:${port}/serviceInfo/ping 2>>/dev/null | grep -q yes ; then
	echo failure at ${PROTOCOL}://${HOST_NAME}:${port}/serviceInfo/ping
       	FAILURE="true"
   fi
done

if [ "${FAILURE}" == "true" ] ; then
	echo FAILURE
	exit 1
else
	exit 0
fi


