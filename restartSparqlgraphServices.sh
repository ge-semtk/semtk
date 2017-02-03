#!/bin/bash
#
# Re-Starts SparqlGraph microservices.
#


# SEMTK = directory holding this script
SEMTK="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

######## STOP #######
$SEMTK/stopSparqlgraphServices.sh

function multips {
	if [ -d /proc ]; then
		grep -a "$1" /proc/*/cmdline | grep -va grep
	else
		ps aux | grep "$1" | grep -v grep
	fi
}

######## WAIT #######
COUNTER=0
while multips 'IngestionService\|QueryService'; do
        let COUNTER=COUNTER+1

        if [ $COUNTER -eq 30 ]; then
                echo "Can't shut down sparqlgraph services."
                exit
        fi

        sleep 1
done

######## START #######
$SEMTK/startSparqlgraphServices.sh
