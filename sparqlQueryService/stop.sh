#!/bin/bash

. ./env.sh

echo ""
echo "Stopping KDL Query Service ..."

docker rm -f ${COMPOSE_PROJECT_NAME}_kdl-query_1

docker ps -a | grep ${COMPOSE_PROJECT_NAME}_kdl-query

