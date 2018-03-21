#!/bin/bash

# Check if docker exists
docker_exists=$(which docker | wc -l | awk '{$1=$1};1')
if [ "${docker_exists}" == "0" ]; then
    echo ""
    echo "This script requires Docker!"
    echo "Please get docker from https://www.docker.com/get-docker and try again."
    echo ""
else
    source .env
    # Generate Maven settings.xml
    cp conf/maven/settings-header.xml-template conf/maven/settings.xml
    if [ ! "$http_proxy" == "" ]; then
    	docker container run --rm -it ${RUN_OPTS} ${VOL_MAP} iankoulski/envsubst:v1.0-alpine3.6 sh -c "cat /wd/conf/maven/settings-proxy.xml-template | envsubst | tee -a /wd/conf/maven/settings.xml"
    fi
    cat conf/maven/settings-footer.xml-template | tee -a conf/maven/settings.xml
    # Compile sources
    docker container run --rm -it ${RUN_OPTS} ${VOL_MAP} ${MVN_IMAGE} bash -c "cd /wd; mvn -s /wd/conf/maven/settings.xml install -DskipTests=true"

fi



