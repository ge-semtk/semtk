#!/bin/bash

export DOCKER_REGISTRY=$DOCKER_REGISTRY
export VERSION=$UBL_VERSION
export COMPOSE_PROJECT_NAME=host

export DNS_HOST_1=$DNS_HOST_1
export DNS_HOST_2=$DNS_HOST_2
export http_proxy=$http_proxy
export https_proxy=$https_proxy
export no_proxy=$no_proxy

export INGEST_PORT=8830

export INGEST_SPARQL_USER=dba
export INGEST_SPARQL_PASSWORD=dba
export INGEST_BATCH_SIZE=5

export INGEST_LOG_YN=NO
export LOG_PROTOCOL=http
export LOG_HOST=server
export LOG_PORT=2440
export LOG_APP_PATH=/Logging/usageLog
export INGEST_LOG_ID=kdl-ingest

