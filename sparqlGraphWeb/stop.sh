#!/bin/bash

. ./env.sh

echo ""
echo "Stopping KDL Sparql UI ..."

docker rm -f ${COMPOSE_PROJECT_NAME}_kdl-sparql_1

docker ps -a | grep ${COMPOSE_PROJECTNAME}_kdl-sparql

