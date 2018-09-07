#!/bin/bash

echo Start this service as stand-alone. Use this for debugging.

export NODEGROUP_SERVICE_MULTIPART_MAXFILESIZE=1000Mb
export PORT_NODEGROUP_SERVICE=12059
echo using port: $PORT_NODEGROUP_SERVICE


java -jar ./target/nodeGroupService-*.jar