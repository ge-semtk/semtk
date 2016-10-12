#!/bin/bash

. ./env.sh

echo ""
echo "Building KDL Ingestion Service ..."

docker build --build-arg DNS_HOST_1=${DNS_HOST_1} --build-arg DNS_HOST_2=${DNS_HOST_2} --build-arg http_proxy=${http_proxy} --build-arg https_proxy=${https_proxy} --build-arg no_proxy=${no_proxy} --tag ${DOCKER_REGISTRY}/kdl-ingest:${VERSION} .

