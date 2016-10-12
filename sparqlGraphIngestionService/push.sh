#!/bin/bash

. ./env.sh

docker push ${DOCKER_REGISTRY}/kdl-ingest:${UBL_VERSION}

