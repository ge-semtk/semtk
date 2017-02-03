#!/bin/bash
#
# End SparqlGraph microservices.
#

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function multikill {
	if [ -d /proc ]; then
		echo windows kill $1...
		kill -9 `grep -a $1 /proc/*/cmdline | grep -va grep  | cut -f 3 -d \/`
	else
		echo pkill -f $1
		pkill -f ruleStorageService
	fi
}

echo "=== END MICROSERVICES... ==="

multikill QueryService
multikill IngestionService

echo "=== DONE ==="
