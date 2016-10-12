#!/bin/bash

. ./env.sh

echo ""
echo "Status of KDL Sparql UI ..."

docker ps -a | grep ${COMPOSE_PROJECTNAME}_kdl-sparql

