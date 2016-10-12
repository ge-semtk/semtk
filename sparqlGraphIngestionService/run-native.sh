#!/bin/bash

java -Dserver.port=$INGEST_PORT -Dingest.sparqlUserName=$INGEST_SPARQL_USER -Dingest.sparqlPassword=$INGEST_SPARQL_PASSWORD -Dingest.batchSize=$INGEST_BATCH_SIZE -Dingest.loggingEnabled=$INGEST_LOG_YN -Dingest.loggingProtocol=$LOG_PROTOCOL -Dingest.loggingServer=${LOG_HOST}${UBL_DOMAIN_SUFFIX} -Dingest.loggingPort=$LOG_PORT -Dingest.loggingServiceLocation=$LOG_APP_PATH -Dingest.applicationLogName=$INGEST_LOG_ID -jar `find . -name sparqlGraphIngestionService*.jar | tail -n1`

