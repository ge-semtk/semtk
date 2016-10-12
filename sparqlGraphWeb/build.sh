#!/bin/bash

. ./env.sh

echo ""
echo "Building kdl-sparql ... "

docker build -t ${DOCKER_REGISTRY}/kdl-sparql:${VERSION} .

