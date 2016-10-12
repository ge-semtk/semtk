#!/bin/bash

# Show arguments
echo "DNS_HOST_1=$DNS_HOST_1, DNS_HOST_2=$DNS_HOST_2, http_proxy=$http_proxy, https_proxy=$https_proxy, no_proxy=$no_proxy"

# Optionally set up proxy
if [ ! -z "${DNS_HOST_1}" ]; then echo "nameserver ${DNS_HOST_1}" > /etc/resolv.conf; fi
if [ ! -z "${DNS_HOST_2}" ]; then echo "nameserver ${DNS_HOST_2}" >> /etc/resolv.conf; fi
if [ ! -z "${http_proxy}" ]; then echo "Acquire::http::proxy \"${http_proxy}\";" > /etc/apt/apt.conf; fi
if [ ! -z "${https_proxy}" ]; then echo "Acquire::https::proxy \"${https_proxy}\";" >> /etc/apt/apt.conf; fi
if [ ! -z "${http_proxy}" ] && [ ! -z "${https_proxy}" ] && [ ! -z "${no_proxy}" ]; then printf "use_proxy=yes\n${http_proxy}\nhttps_proxy=${https_proxy}\nno_proxy=${no_proxy}" > /root/.wgetrc; fi
if [ -f /etc/apt/apt.conf ]; then cat /etc/apt/apt.conf; fi

# Start service
./run-native.sh

