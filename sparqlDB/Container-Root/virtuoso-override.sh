#!/bin/bash
SETTINGS_DIR=/settings
mkdir -p $SETTINGS_DIR

cd /data

mkdir -p dumps

if [ ! -f ./virtuoso.ini ];
then
  mv /virtuoso.ini . 2>/dev/null
fi

chmod +x /clean-logs.sh
mv /clean-logs.sh . 2>/dev/null

original_port=`crudini --get virtuoso.ini HTTPServer ServerPort`
# NOTE: prevents virtuoso to expose on port 8890 before we actually run
#the server
crudini --set virtuoso.ini HTTPServer ServerPort 27015

if [ ! -f "$SETTINGS_DIR/.config_set" ];
then
  echo "Converting environment variables to ini file"
  printenv | grep -P "^VIRT_" | while read setting
  do
    section=`echo "$setting" | grep -o -P "^VIRT_[^_]+" | sed 's/^.\{5\}//g'`
    key=`echo "$setting" | grep -o -P "_[^_]+=" | sed 's/[_=]//g'`
    value=`echo "$setting" | grep -o -P "=.*$" | sed 's/^=//g'`
    echo "Registering $section[$key] to be $value"
    crudini --set virtuoso.ini $section $key "$value"
  done
  echo "`date +%Y-%m%-dT%H:%M:%S%:z`" >  $SETTINGS_DIR/.config_set
  echo "Finished converting environment variables to ini file"
fi

if [ ! -f ".dba_pwd_set" ];
then
  touch /sql-query.sql
  if [ "$DBA_PASSWORD" ]; then echo "user_set_password('dba', '$DBA_PASSWORD');" >> /sql-query.sql ; fi
  if [ "$SPARQL_UPDATE" = "true" ]; then echo "GRANT SPARQL_UPDATE to \"SPARQL\";" >> /sql-query.sql ; fi
  virtuoso-t +wait && isql-v -U dba -P dba < /dump_nquads_procedure.sql && isql-v -U dba -P dba < /sql-query.sql
  kill "$(ps aux | grep '[v]irtuoso-t' | awk '{print $2}')"
  echo "`date +%Y-%m-%dT%H:%M:%S%:z`" >  .dba_pwd_set
fi

if [ ! -f ".data_loaded" -a -d "toLoad" ] ;
then
    echo "Start data loading from toLoad folder"
    graph="http://localhost:8890/DAV"
    passwd="dba"
    if [ "$DBA_PASSWORD" ]; then passwd="$DBA_PASSWORD" ; fi
    if [ "$DEFAULT_GRAPH" ]; then graph="$DEFAULT_GRAPH" ; fi
    # Load each file path from toLoad dir into its own graph
    for file_path in $(find toLoad -type f); do
        dir_name=$(dirname "$file_path")
        subgraph=${dir_name:7}
        file_name=$(basename "$file_path")
	echo "Loading file: $file_path to graph $graph/$subgraph ..."
    	echo "ld_dir('$dir_name', '$file_name', '$graph/$subgraph');" > /load_data.sql
        echo "rdf_loader_run();" >> /load_data.sql
        echo "exec('checkpoint');" >> /load_data.sql
        echo "WAIT_FOR_CHILDREN; " >> /load_data.sql
        echo "$(cat /load_data.sql)"
        virtuoso-t +wait && isql-v -U dba -P "$passwd" < /load_data.sql
        kill $(ps aux | grep '[v]irtuoso-t' | awk '{print $2}')
        rm -f /load_data.sql
    done
    echo "`date +%Y-%m-%dT%H:%M:%S%:z`" > .data_loaded
else
    echo "Data loaded on `cat .data_loaded`"
fi

crudini --set virtuoso.ini HTTPServer ServerPort ${VIRT_HTTPServer_ServerPort:-$original_port}

exec virtuoso-t +wait +foreground

