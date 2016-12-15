#!/bin/bash
#
# Re-Starts SparqlGraph microservices.
#


# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

######## STOP #######
$SEMTK/stopSparqlgraphServices.sh

######## WAIT #######
COUNTER=0
while ps aux | grep 'IngestionService\|QueryService\|ontologyInfoService\|NodeGroupStore' | grep -v grep; do
        let COUNTER=COUNTER+1

        if [ $COUNTER -eq 30 ]; then
                echo "Can't shut down sparqlgraph services."
                exit
        fi

        sleep 1
done

######## START #######
$SEMTK/startSparqlgraphServices.sh
