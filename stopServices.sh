#!/bin/bash
#
# Stop microservices. 
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
		pkill -f $1
	fi
}

multikill sparqlQueryService
multikill sparqlGraphIngestionService
multikill ontologyInfoService
multikill nodeGroupStoreService
multikill sparqlGraphStatusService
multikill sparqlGraphResultsService
multikill hiveService
multikill oracleService
multikill sparqlExtDispatchService
multikill rdbTimeCoherentTimeSeriesQueryGenService
multikill nodeGroupExecutionService
multikill nodeGroupService

echo "=== DONE ==="
