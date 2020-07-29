#!/bin/bash
#
# Configures SemTK services and webapps
#
# Usage: Run from any directory.  First argument must be webapps install directory.
#

# Stop if any command goes bad
set -e

# Validate first argument
if [ "$#" -ne 1 ]; then
    echo "Usage: ${BASH_SOURCE[0]} DIRECTORY"
    exit 1
fi
WEBAPPS="$1"
if [ ! -d "${WEBAPPS}" ]; then
    echo "WEBAPPS is not a valid directory: ${WEBAPPS}"
    exit 1
fi

# DIR = directory holding this script
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd -P)"
cd "${DIR}"

# If running in Docker and SERVER_ADDRESS is not set, then set
# SERVER_ADDRESS to localhost before sourcing .env
if [ -f /.dockerenv ]; then
    export SERVER_ADDRESS="${SERVER_ADDRESS:-localhost}"
fi
source .env

# Replace the environment file for the SemTK services
cat ./*Service/BOOT-INF/classes/application.properties | grep "{" | sed -e 's/^.*{\(.*\)}$/\1=${\1}/' | sort | uniq | envsubst > environment

# Replace the js configuration files for the SemTK webapps
./configWebapps.sh "${WEBAPPS}"
