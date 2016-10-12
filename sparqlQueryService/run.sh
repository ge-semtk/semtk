#!/bin/bash

. ./env.sh

echo ""
echo "Starting KDL Query Service ..."

docker run -d --name ${COMPOSE_PROJECT_NAME}_kdl-query_1 -e QUERY_PORT=$QUERY_PORT -p $QUERY_PORT:$QUERY_PORT ${DOCKER_REGISTRY}/kdl-query:${UBL_VERSION}

docker ps -a | grep ${COMPOSE_PROJECT_NAME}_kdl-query

