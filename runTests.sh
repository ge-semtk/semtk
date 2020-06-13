#!/bin/bash
#
# Copyright 2017 General Electric Company
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
#
# Runs JUnit unit and integration tests.
#
# Usage: Run from any directory.  Takes no arguments.
#
# NOTE: services must be running for integration tests to pass
#

set -o nounset          # exit if any variable not set

DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)  # the directory containing the script
TIMESTAMP=$(date "+%Y%m%dT%H%M%S")
LOG=$DIR/runTests-$TIMESTAMP.log 
echo "Writing full test output to "$LOG

cd $DIR
echo "Running unit tests in $DIR..."
mvn test &>> $LOG 
echo "Running integration tests in $DIR..."
. ./.env
mvn failsafe:integration-test failsafe:verify &>> $LOG

# print summary to console
cat $LOG | grep "Tests run:" | grep -v "Time elapsed"
