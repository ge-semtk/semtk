#!/bin/bash

. ./env.sh

echo ""
echo "Tailing KDL Query Service Log ..."

docker logs -f ${COMPOSE_PROJECT_NAME}_kdl-query_1

