#!/bin/bash

. ./env.sh

echo ""
echo "Stopping KDL Ingestion Service ..."

docker rm -f ${COMPOSE_PROJECT_NAME}_kdl-ingest_1

docker ps -a | grep ${COMPOSE_PROJECT_NAME}_kdl-ingest_1

