#!/bin/bash
#
# End SparqlGraph microservices.
#

# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=== END MICROSERVICES... ==="

pkill -f QueryService
pkill -f IngestionService

echo "=== DONE ==="
