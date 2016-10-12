#!/bin/bash

. ./env.sh

docker push ${DOCKER_REGISTRY}/kdl-sparql:${VERSION}

