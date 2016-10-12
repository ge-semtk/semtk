#!/bin/bash

. ./env.sh

echo ""
echo "Starting KDL sparqlGraph UI ..."

docker run -d --name ${COMPOSE_PROJECT_NAME}_kdl-sparql_1 -e INGEST_URL=$INGEST_URL -e QUERY_URL=$QUERY_URL -p $SPARQL_PORT:8080 ${DOCKER_REGISTRY}/kdl-sparql:${VERSION}

docker ps -a | grep ${COMPOSE_PROJECT_NAME}_kdl-sparql

