#!/bin/bash

. ./env.sh

echo ""
echo "Starting KDL Ingestion Service ..."

docker run -d --name ${COMPOSE_PROJECT_NAME}_kdl-ingest_1 -e INGEST_PORT=$INGEST_PORT -e INGEST_SPARQL_PASSWORD=$INGEST_SPARQL_PASSWORD -e INGEST_BATCH_SIZE=$INGEST_BATCH_SIZE -e INGEST_LOG_YN=$INGEST_LOG_YN -e LOG_PROTOCOL=$LOG_PROTOCOL -e LOG_HOST=$LOG_HOST -e UBL_DOMAIN_SUFFIX=$UBL_DOMAIN_SUFFIX -e LOG_PORT=$LOG_PORT -e LOG_APP_PATH=$LOG_APP_PATH -e INGEST_LOG_ID=$INGEST_LOG_ID -e http_proxy=$http_proxy -e https_proxy=$https_proxy -e no_proxy=$no_proxy -p $INGEST_PORT:$INGEST_PORT ${DOCKER_REGISTRY}/kdl-ingest:${UBL_VERSION}

docker ps | grep ${COMPOSE_PROJECT_NAME}_kdl-ingest_1

