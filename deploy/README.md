# SemTK via Docker

## Build Docker image (using Packer)

Install [Packer](https://www.packer.io/) if you don't have it. 

Manually download or copy the files below into the `files` subdirectory before building:

- `packer/files/apache-jena-fuseki-3.16.0.tar.gz`: latest Fuseki release
  (download it from <https://jena.apache.org/download/>)

- `packer/files/semtk-opensource-2.2.1-SNAPSHOT-bin.tar.gz`: latest SemTK
  distribution (clone semtk-opensource, run ./build.sh, and copy
  distribution/target/*.tar.gz to files/)

- `packer/files/systemctl3.py`: entrypoint and init daemon (visit
  [docker-systemd-replacement](https://github.com/gdraheim/docker-systemctl-replacement)
  and download files/docker/systemctl3.py to files/ under the European
  Union Public Licence).  Current working version is v1.5.4260.

The following command will build, commit, and tag the Docker image.  

- If proxy not required: `packer build semtk-docker.json`

- If proxy required, set http\_proxy and https\_proxy environment variables and then run: `packer build -var "http_proxy=${http_proxy}" -var "https_proxy=${https_proxy}" semtk-docker.json`

Note: Packer supports building images for many hypervisors and clouds (e.g. Docker, Hyper-V, 
VirtualBox).  Currently only Docker files are provided here.  More may be added in the future.

## Run Docker container

The following command will create and run a Docker container.  The file `env.list` contains environment variables to be set at deploy time (e.g. to specify the semantic store to connect to) - these may be modified per deployment.

`docker run --detach -p 80:80 -p 12050-12092:12050-12092 --env-file env.list semtk`

Access SPARQLgraph at [http://localhost](http://localhost)

## Notes

- SemTK service logs are available at this location within the container: `/var/log/journal/*.service.log`

- To bind container port (e.g. 80) to a different port (e.g. 81) on the host machine, replace `-p 80:80` with `-p 81:80`
