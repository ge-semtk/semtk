#!/bin/bash

# This script is the entrypoint of the kdl-sparql Docker image
# It is reposnsible for configuring the web application and starting it
# It relies on environment variables that should be defined before this script executes

# Helper function from .fun
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

# Configure app
echo ""
echo "Configuring sparqlGraph UI ..."

FILE_PATHS=(
  "/usr/local/tomcat/webapps/sparqlGraph/main-oss/sparqlgraphconfigOss.js"
  "/usr/local/tomcat/webapps/sparqlForm/main-oss/sparqlformconfig.js "
)

for f in ${FILE_PATHS[@]}; do
  replace_vars_in_file ${f} "^WEB_"
done

# Start tomcat
echo ""
echo "Starting Tomcat ..."
/usr/local/tomcat/bin/catalina.sh run

