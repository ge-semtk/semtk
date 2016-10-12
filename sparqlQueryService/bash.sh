#!/bin/bash

. ./env.sh

echo ""
echo "Opening KDL Query Service shell ..."

docker exec -it ${COMPOSE_PROJECT_NAME}_kdl-query_1 /bin/bash

