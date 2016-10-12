#!/bin/bash

. ./env.sh

echo ""
echo "Tailing KDL Ingestion Service Log ..."

docker logs -f ${COMPOSE_PROJECT_NAME}_kdl-ingest_1

