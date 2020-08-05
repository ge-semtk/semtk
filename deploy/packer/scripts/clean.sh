#!/bin/bash

# Disable interactive configuration steps

export DEBIAN_FRONTEND=noninteractive
export DEBCONF_NONINTERACTIVE_SEEN=true

# Remove linux-cloud-tools-generic unless we're building a Hyper-V VM

if [ "${PACKER_BUILDER_TYPE}" != "hyperv-iso" ]; then
    apt-get remove -y linux-cloud-tools-generic
    apt-get autoremove -y
fi

# Upgrade all packages

apt-get update
apt-get upgrade -y

# Clean apt cache and temporary files

apt-get clean -y
rm -rf /tmp/files /var/lib/apt/lists/*

# Execute this part of the script only if we have a virtual hard disk

if [ "${PACKER_BUILDER_TYPE}" != "docker" ]; then

    # Zero out the rest of the free space using dd, then delete the written file

    dd if=/dev/zero of=/EMPTY bs=1M
    rm -f /EMPTY

    # Add `sync` so Packer doesn't quit too early, before the large file is deleted

    sync

fi
