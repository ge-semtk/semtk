#!/bin/bash
#
# Backs up a Fuseki dataset to a compressed N-Quads file
# 
# Sample use: backupFusekiDataset.sh MY_DATASET
# Sample backup file produced: apache-jena-fuseki-4.5.0/run/backups/MY_DATASET_2023-10-12_06-57-45.nq.gz
#
# Returns non-zero error code if: Fuseki not running, wrong number of arguments, invalid dataset
# If Fuseki is not password protected, then works with $USER and $PASSWORD undefined
#

set -e                  # exit if any command fails (returns non-true value)

# validate number of parameters
if [ $# -ne 1 ]; then
    echo "Illegal number of parameters.  Usage: backupFusekiDataset.sh MY_DATASET"
	exit 1;
fi

DATASET=$1		# Fuseki dataset to back up

# print arguments
echo "Backup Fuseki dataset"
echo "DATASET: $DATASET"

# execute backup
output_json=$(curl -s --user "$USER:$PASSWORD" -XPOST http://localhost:3030/$/backup/$DATASET)
echo $output_json

# check output
if [[ $output_json = "Dataset not found" ]]; then
	exit 1;
fi
# TODO could get task id, wait for completion, confirm successful

echo "Done"