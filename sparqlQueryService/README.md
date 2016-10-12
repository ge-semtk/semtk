# semTK sparqlGraph Query Service

This service provides REST endpoints for querying data from a graph database

## License
  SPARQLgraph
  Knowledge Discovery Lab
  GE Research, Niskayuna
  
  Copyright � 2014-2016 General Electric Company.
  Licensed under the Apache License, Version 2.0 (the "License")
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
  
  Patents Pending.
  
  Includes other open source code:
  Raphael 1.3.1 - JavaScript Vector Library.
  Distributed under MIT license.
  - Copyright (c) 2008 - 2009 Dmitry Baranovskiy (http://raphaeljs.com)
  
  Dracula Graph Layout and Drawing Framework 0.0.3alpha.
  Distributed under MIT license.
  - (c) 2010 Philipp Strathausen , http://strathausen.eu Contributions by Jake Stothard .
  based on the Graph JavaScript framework, version 0.0.1
  - (c) 2006 Aslak Hellesoy
  - (c) 2006 Dave Hoover
  
  Curry - Function currying.
  Licensed under BSD (http://www.opensource.org/licenses/bsd-license.php)
  - Copyright (c) 2008 Ariel Flesler - aflesler(at)gmail(dot)com | http://flesler.blogspot.com

## Building from source
Use maven and the included pom.xml to build the source code. 
`  mvn install`

## Running natively
The run-native.sh script starts the Query Service natively using java and the compiled jar file.

## Docker support
After the jar file is built from source, it can be packaged in a Docker container using the provided build.sh script.
The following Docker-related scripts are provided:
* env.sh - defines environment variables used by other scripts
* build.sh - builds the Docker image
* run.sh - runs this service in a Docker container
* logs.sh - shows Docker container logs
* stop.sh - stops and removes the Docker container
* push.sh - pushes the image to a Docker registry
* pull.sh - pulls the image from a Docker registry
* status.sh - shows whether a Docker container is present and its status
* bash.sh - opens a bash shell into the Docker container while it is running
* entrypoint.sh - script that gets embedded in the Docker image, meant for execution inside the container only for starting the service

