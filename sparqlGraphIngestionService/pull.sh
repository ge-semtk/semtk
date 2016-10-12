#!/bin/bash

. ./env.sh

docker pull ${DOCKER_REGISTRY}/kdl-ingest:${UBL_VERSION}

