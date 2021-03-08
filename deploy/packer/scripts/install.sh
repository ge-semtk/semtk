#!/bin/bash

# Exit if anything goes wrong

set -e
cd /tmp/files

# Execute this part of the script only if we're building a Docker image

if [ "${PACKER_BUILDER_TYPE}" == "docker" ]; then

    # Install necessary packages

    export DEBIAN_FRONTEND=noninteractive
    export DEBCONF_NONINTERACTIVE_SEEN=true

    apt-get update -y
    apt-get install -y curl default-jre gettext-base nano nginx-light python3 unzip

    # Install docker-systemctl-replaement

    chmod 755 systemctl3.py
    mv systemctl3.py /usr/bin
    ln -sf systemctl3.py /usr/bin/systemctl

    # Create ubuntu user

    adduser --disabled-password --gecos '' ubuntu

fi

# Unpack the Fuseski distribution

tar xfzC apache-jena-fuseki-3.16.0.tar.gz /opt
mv /opt/apache-jena-fuseki-3.16.0 /opt/fuseki

# Set up and start Fuseki system service

adduser --system --group --no-create-home --disabled-password fuseki
mkdir /etc/fuseki
chown fuseki.fuseki /etc/fuseki
cp /opt/fuseki/fuseki.service /etc/systemd/system/fuseki.service
systemctl enable fuseki
systemctl start fuseki

# Unpack the SemTK distribution

export USER=ubuntu
tar xfzC semtk-opensource-dist.tar.gz /home/${USER}
#mv ENV_OVERRIDE /home/${USER}/semtk-opensource
cd /home/${USER}/semtk-opensource

# Initialize SemTK environment variables

export SERVER_ADDRESS=localhost
source .env

# Set up each SemTK system service

for dir in *Service; do
  (
    if [[ "$ENABLED_SERVICES" == *"$dir"* ]]; then
       cd "$dir"
       SERVICE=$(basename "${PWD}")
       export SERVICE
       unzip -q target/*.jar
       rm -rf pom.xml target
       envsubst <../service.unit >/etc/systemd/system/"${SERVICE}".service
       systemctl enable "${SERVICE}"
    fi
  )
done

# Install the SemTK webapps 

export WEBAPPS=/var/www/html
./updateWebapps.sh ${WEBAPPS}
# move SemTK index.html into place (updateWebapps.sh mistakenly creates a ROOT file, since /var/www/html does not have a ROOT directory like in Tomcat)
mv /var/www/html/ROOT /var/www/html/index.html

# Change file ownerships since all SemTK code runs as non-root ${USER}

chown -R ${USER}.${USER} /home/${USER}
chown -R ${USER}.${USER} ${WEBAPPS}

# Configure the SemTK services and webapps

envsubst <configSemTK.service >/etc/systemd/system/configSemTK.service
systemctl enable configSemTK
systemctl start configSemTK

# Create and initialize the Fuseki database (server URL will be "http://localhost:3030/SEMTK") 

curl -Ss -d 'dbName=SEMTK' -d 'dbType=tdb' 'http://localhost:3030/$/datasets'
# How to load a data file (keep for future reference)
#curl -Ss -F 'files[]=@/tmp/files/SEMTK.nq' 'http://localhost:3030/SEMTK/data'

