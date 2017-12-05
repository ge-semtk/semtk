#!/bin/bash
#
# Runs JUnit unit and integration tests.
#
# Usage: Run from any directory.  Takes no arguments.
#
# NOTE: services must be running for integration tests to pass
#

set -o nounset          # exit if any variable not set
set -e                  # exit if any command returns non-true return value

DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)  # the directory containing the script
TIMESTAMP=$(date "+%Y%m%dT%H%M%S")
LOG=$DIR/runTests-$TIMESTAMP.log 
echo "Writing full test output to "$LOG

cd $DIR
echo "Running unit tests in $DIR..."
mvn test &>> $LOG 
echo "Running integration tests in $DIR..."
mvn failsafe:integration-test &>> $LOG 

# print summary to console
cat $LOG | grep "Tests run:" | grep -v "Time elapsed"
