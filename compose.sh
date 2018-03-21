#!/bin/bash
#
# Copyright 2018 General Electric Company
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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

