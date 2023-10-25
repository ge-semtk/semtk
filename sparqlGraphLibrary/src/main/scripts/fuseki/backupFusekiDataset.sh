#!/bin/bash
#
# Backs up a Fuseki dataset to a compressed N-Quads file
# 
# Sample use: backupFusekiDataset.sh MY_DATASET
# 
# If Fuseki is password protected, set environment variables $FUSER and $FPASS before running this script.
# Returns non-zero error code if: invalid parameters, Fuseki not running, invalid credentials, invalid dataset, too long without completion 
# 
# Backup file is created under Fuseki directory, e.g. apache-jena-fuseki-4.5.0/run/backups/MY_DATASET_2023-10-12_06-57-45.nq.gz
# To restore using backup file, use the Fuseki UI.  The graphs within the dataset (including default graph) are preserved.
#

set -e                  # exit if any command fails (returns non-true value)

### validate number of parameters
if [ $# -ne 1 ]; then
    echo "Illegal number of parameters.  Usage: backupFusekiDataset.sh MY_DATASET"
	exit 1;
fi

DATASET=$1
FUSEKI_LOCATION=http://localhost:3030

echo "Backup Fuseki dataset"
echo "FUSER: $FUSER"
echo "DATASET:     $DATASET"

### initiate backup
### if Fuseki not running, script exits with error code 7
output=$(curl -s --user "$FUSER:$FPASS" -XPOST $FUSEKI_LOCATION/$/backup/$DATASET)
echo "Response from Fuseki: " $output        # should look like { "taskId" : "19" , "requestId" : 70 }

### check for blank response - often caused by missing credentials
if [[ $output == "" ]]; then
	echo "Response is blank.  If Fuseki is password protected, be sure that environment variables \$FUSER and \$FPASS are set."
	exit 1;
fi

###  extract taskId
if jq -e . >/dev/null 2>&1 <<<"$output"; then  # if can parse json
	taskId=$( echo $output | jq .taskId | sed -r 's/\"//g' )
	if [[ $taskId == "" ]]; then
		echo "Cannot retrieve task id"
		exit 1;
	fi
else
	# exit with error if parsing failed (e.g. for response "Dataset not found")
    echo "Backup failed"
    exit 1;
fi

### poll task status for success
count=0
while [[  $success = "" || $success = "null" ]]
do
	success=$(curl -s --user "$FUSER:$FPASS" $FUSEKI_LOCATION/$/tasks/$taskId | jq .success)
	sleep 2
	echo -n "."  # dots to indicate waiting
	((count=count+1))
	if [[ $count == 300 ]]; then   # give it 10 minutes
		# taking too long
		echo "Exiting, backup result unknown"
		exit 1;
	fi 
done
echo ""

### succeeded or not
if [[ $success = "false" ]]; then
	echo "Backup failed"
	exit 1;
elif [[ $success = "true" ]]; then
	echo "Backup complete"
	exit 0;
fi
