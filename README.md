
# semtk
Semantics Toolkit.   Auto-SPARQL-query generation.  Drag-and-drop ingestion.

Try out the web apps at:  [semtk.research.ge.com](http://semtk.research.ge.com).

Full info is in the [wiki](https://github.com/ge-semtk/semtk/wiki/Home)

## Building semtk from source
The complexities of setting up a build environment and resolving dependencies while compiling the code have been removed by using a "depend-on-docker" approach, where the only dependency or requirement for the successful build of the project is having a (Docker)[https://www.docker.com] installation. To compile the source code, simply install (Docker)[https://www.docker.com/get-docker] and run the compile.sh script:

    ./compile.sh

All sources will be compiled and the jar files will be saved in the "target" folder of each service as well as deployed to the local Maven repository.

## Running semtk
Semantics Toolkit services can be run natively on the host or started within Docker containers.
### Run using Java
To run all semtk services on the host using Java, execute: 

    ./startServices.sh

### Run using Docker
If Docker is available, all services can be run in containers. Container lifecycles in this project are managed by the ./compose.sh script. Before containers can be started, the container images need to be built:

    ./compose.sh build

then simply bring up the services using the same compose.sh script. This script uses docker-compose, which actually runs in Docker.

    ./compose.sh up -d

#### Show running containers

    ./compose.sh ps

#### Show an aggregated log for all micro-services

    ./compose.sh logs -f 

##### Show logs for a particular service

    ./compose.sh logs -f <service-name>

where service name is the nameo of the service (example semtk-nodegroup-store)

#### Stop all services

    ./compose.sh down

#### See other available commands

    ./compose.sh --help
