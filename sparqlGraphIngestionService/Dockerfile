# MIA Recommender Service
FROM java:8

MAINTAINER Justin McHugh <mchugh@ge.com>

# Define build arguments
ARG DNS_HOST_1
ARG DNS_HOST_2
ARG http_proxy
ARG https_proxy
ARG no_proxy

# Show arguments
RUN echo "DNS_HOST_1=$DNS_HOST_1, DNS_HOST_2=$DNS_HOST_2, http_proxy=$http_proxy, https_proxy=$https_proxy, no_proxy=$no_proxy"

# Optionally set up proxy
RUN if [ ! -z "${DNS_HOST_1}" ]; then echo "nameserver ${DNS_HOST_1}" > /etc/resolv.conf; fi
RUN if [ ! -z "${DNS_HOST_2}" ]; then echo "nameserver ${DNS_HOST_2}" >> /etc/resolv.conf; fi
RUN if [ ! -z "${http_proxy}" ]; then echo "Acquire::http::proxy \"${http_proxy}\";" > /etc/apt/apt.conf; fi
RUN if [ ! -z "${https_proxy}" ]; then echo "Acquire::https::proxy \"${https_proxy}\";" >> /etc/apt/apt.conf; fi
RUN if [ ! -z "${http_proxy}" ] && [ ! -z "${https_proxy}" ] && [ ! -z "${no_proxy}" ]; then printf "use_proxy=yes\n${http_proxy}\nhttps_proxy=${https_proxy}\nno_proxy=${no_proxy}" > /root/.wgetrc; fi
RUN if [ -f /etc/apt/apt.conf ]; then cat /etc/apt/apt.conf; fi

# Update system
RUN apt-get update
RUN apt-get upgrade -y

# Install basic tools
RUN apt-get install -y curl wget unzip vim-tiny nano

# Add content
RUN mkdir -p /localadm/kdl-ingest

COPY target/*.jar /localadm/kdl-ingest

COPY run-native.sh /localadm/kdl-ingest/

COPY entrypoint.sh /localadm/kdl-ingest/

COPY LICENSE.txt /localadm/kdl-ingest/

WORKDIR /localadm/kdl-ingest

ENTRYPOINT /localadm/kdl-ingest/entrypoint.sh

