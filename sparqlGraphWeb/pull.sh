#!/bin/bash

. ./env.sh

docker pull ${DOCKER_REGISTRY}/kdl-sparql:${VERSION}

