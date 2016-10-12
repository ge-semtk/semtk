#!/bin/bash

. ./env.sh

echo ""
echo "Opening KDL Ingestion Service shell ..."

docker exec -it ${COMPOSE_PROJECT_NAME}_kdl-ingest_1 /bin/bash

