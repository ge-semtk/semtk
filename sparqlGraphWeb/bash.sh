#!/bin/bash

. ./env.sh

echo ""
echo "Opening KDL Sparql UI shell ..."

docker exec -it ${COMPOSE_PROJECT_NAME}_kdl-sparql_1 /bin/bash

