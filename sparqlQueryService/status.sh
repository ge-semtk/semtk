#!/bin/bash

. ./env.sh

echo ""
echo "Status of KDL Query Service ..."

docker ps -a | grep ${COMPOSE_PROJECT_NAME}_kdl-query

