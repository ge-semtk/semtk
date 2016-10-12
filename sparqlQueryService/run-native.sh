#!/bin/bash

java -Dserver.port=$QUERY_PORT -jar `find . -name sparqlQueryService*.jar | tail -n1`

