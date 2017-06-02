#!/bin/bash

export DOCKER_REGISTRY=$DOCKER_REGISTRY
export VERSION=$UBL_VERSION
export COMPOSE_PROJECT_NAME=host

export SPARQL_PORT=8860

export INGEST_URL=http://vesuvius37.crd.ge.com:12091/ingestion/
export QUERY_URL=http://vesuvius37.crd.ge.com:12050/sparqlQueryService/
