#!/bin/bash

# Helper functions
## Detect current operating system
function os
{
        UNAME=$(uname -a)
        if [ $(echo $UNAME | awk '{print $1}') == "Darwin" ]; then
                export OPERATING_SYSTEM="MacOS"
        elif [ $(echo $UNAME | awk '{print $1}') == "Linux" ]; then
                export OPERATING_SYSTEM="Linux"
        elif [ ${UNAME:0:5} == "MINGW" ]; then
                export OPERATING_SYSTEM="Windows"
                export MSYS_NO_PATHCONV=1 # turn off path conversion
        else
                export OPERATING_SYSTEM="Other"
        fi
}
## End os function

## Determine current host IP address
function hostip
{
        case "${OPERATING_SYSTEM}" in
        "Linux")
                export HOST_IP=$(hostname -I | tr " " "\n" | head -1) # Linux
                ;;
        "MacOS")
                export HOST_IP=$(ifconfig | grep -v 127.0.0.1 | grep -v inet6 | grep inet | head -n 1 | awk '{print $2}') # Mac OS
                ;;
        "Windows")
                export HOST_IP=$( ((ipconfig | grep IPv4 | grep 10.187 | tail -1) && (ipconfig | grep IPv4 | grep 3. | head -1)) | tail -1 | awk '{print $14}' ) # Git bash
                ;;
        *)
                export HOST_IP=$(hostname)
                ;;
        esac
}
## End hostip function

## Determine current host name
function hostname
{
        case "${OPERATING_SYSTEM}" in
        "Linux")
				# fully qualified, e.g. host.company.com
                export HOST_NAME="$(host $HOST_IP | awk '{print substr($NF, 1, length($NF)-1)}')" # Linux
                ;;
        *)
				# TODO plain hostname for now - in future may need to differentiate between OS and/or get fully qualified hostname
                export HOST_NAME=$(hostname)
                ;;
        esac
}
## End hostname function


# @author 200001934 Paul
#
# Replaces environment variables in text files
#
# FILE_PATH - file path
# VAR_PATTERN - use environment variables matching this pattern
# VAR_NAME_SEARCH  \
# VAR_NAME_REPLACE /  perform this search/replace on the variable name before checking the file
#
# e.g.  replace_vars_in_file "file.txt" "WEB1_LOC" "1" ""
#       finds every environment var containing WEB1_LOC  (WEB1_LOC_WHATEVER...)
#       searches file.txt replacing ${WEB_LOC_WHATEVER} (<- the 1 is missing) with the value of $WEB1_LOC_WHATEVER

replace_vars_in_file() {
	FILE_PATH=$1
	VAR_PATTERN=$2
	VAR_NAME_SEARCH=$3
	VAR_NAME_REPLACE=$4

	# if VAR_NAME_SEARCH is empty, replace Y with Y: no effect
	if [ -z "$VAR_NAME_SEARCH" ]; then
		VAR_NAME_SEARCH=Y
		VAR_NAME_REPLACE=Y
	fi

	# get a list of env variables matching VAR_PATTERN
	# modifying the variable names by VAR_NAME_SEARCH / VAR_NAME_REPLACE
	names=($(printenv | grep ${VAR_PATTERN} | cut -f1 -d= | sed "s#${VAR_NAME_SEARCH}#${VAR_NAME_REPLACE}#g" ))
	IFS=$'\n'
	values=($(printenv | grep ${VAR_PATTERN} | cut -f2 -d= ))
	unset IFS

	# replace each variable value into the file in place
	# it must be exactly of the form ${VAR_NAME}
	echo Configuring ${FILE_PATH}...
	for ((i=0; $i<${#names[*]}; i++)); do
		FIND='${'${names[$i]}'}'
		REPL="${values[$i]}"
		set -x
		sed --in-place "s#${FIND}#${REPL}#g" "${FILE_PATH}"
		set +x
	done
}
## End replace_vars_in_file function
