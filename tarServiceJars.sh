#!/bin/bash
#
# Copyright 2018 General Electric Company
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
# Creates a tar file containing deployable service jars (and start script) in the maven directory structure.
#
# Usage: Run from any directory. Takes no arguments.
#

set -o nounset          # exit if any variable not set
set -e                  # exit if any command returns non-true return value

DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)  # the directory containing the script
TIMESTAMP=$(date "+%Y%m%dT%H%M%S")

SEMTK_OPENSOURCE=semtk-opensource							# top-level SemTK directory
DIR_TO_TAR=semtkServiceJars-$SEMTK_OPENSOURCE-$TIMESTAMP	# create temp directory

cd $DIR
echo "Gathering SemTK service jars from $DIR..."

# make temporary directory to tar
mkdir $DIR_TO_TAR
mkdir $DIR_TO_TAR/$SEMTK_OPENSOURCE

# copy in all jars and needed scripts
cp --parents */target/*.jar $DIR_TO_TAR/$SEMTK_OPENSOURCE
cp --parents startServices.sh $DIR_TO_TAR/$SEMTK_OPENSOURCE

# remove unneeded jars
rm -rf $DIR_TO_TAR/$SEMTK_OPENSOURCE/standaloneExecutables/ 
rm -rf $DIR_TO_TAR/$SEMTK_OPENSOURCE/sparqlGraphLibrary/

# create tar file (and remove the temp directory)
tar zcvf $DIR_TO_TAR.tar.gz $DIR_TO_TAR
rm -rf $DIR_TO_TAR

echo "Created tar file $DIR_TO_TAR.tar.gz..."