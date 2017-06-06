#!/bin/bash
#
# Stop microservices, including the ones needed for SparqlGraph. 
#

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=== END MICROSERVICES... ==="

function multikill {
	if [ -d /proc ]; then
		echo windows kill $1...
		kill -9 `grep -a $1 /proc/*/cmdline | grep -va grep  | cut -f 3 -d \/`
	else
		echo pkill -f $1
		pkill -f ruleStorageService
	fi
}

# stop SPARQL query service, ingestion service
"$SEMTK"/stopSparqlgraphServices.sh

multikill ontologyInfoService
multikill odeGroupStoreService
multikill sparqlGraphStatusService
multikill sparqlGraphResultsService
multikill hiveService
multikill oracleService
multikill sparqlExtDispatchService
multikill rdbTimeCoherentTimeSeriesQueryGenService
multikill storedNodegroupExecutionService

echo "=== DONE ==="
