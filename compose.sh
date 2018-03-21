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
    ENV_FILE=$(mktemp /tmp/env.XXXXXXXXX)
    env > ${ENV_FILE}
    docker container run --rm -it -v /var/run/docker.sock:/var/run/docker.sock -v $(pwd):/wd --env-file ${ENV_FILE} docker/compose:1.19.0 -f /wd/compose.yml ${@}
    rm -f ${ENV_FILE}
fi

