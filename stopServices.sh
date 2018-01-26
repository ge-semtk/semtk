#!/bin/bash
#
# Copyright 2016 General Electric Company
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
