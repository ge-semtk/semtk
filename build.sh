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
# Builds all sources from scratch.
#
# Usage: Run from any directory.  Takes no arguments.
#
# NOTE: Removed -Djavax.net.ssl.trustStore=trust.jks  -Djavax.net.ssl.trustStorePassword=password since they didn't seem to be needed
#

DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)  # the directory containing the script
cd $DIR
echo "Building all sources in $DIR..."
mvn clean install -DskipTests
