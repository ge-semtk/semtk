#!/bin/bash
#
# Stop microservices, including the ones needed for SparqlGraph. 
#

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=== END MICROSERVICES... ==="

./stopSparqlgraphServices.sh
pkill -f ontologyInfoService
pkill -f nodeGroupStoreService
pkill -f sparqlGraphStatusService
pkill -f sparqlGraphResultsService
pkill -f hiveService
pkill -f oracleService

echo "=== DONE ==="
