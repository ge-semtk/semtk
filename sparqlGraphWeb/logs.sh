#!/bin/bash

. ./env.sh

echo ""
echo "Tailing KDL Sparql Log ..."

docker logs -f ${COMPOSE_PROJECT_NAME}_kdl-sparql_1

