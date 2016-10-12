#!/bin/bash

. ./env.sh

docker push ${DOCKER_REGISTRY}/kdl-query:${VERSION}

