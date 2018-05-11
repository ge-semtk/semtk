#!/bin/sh

# Container startup script

echo ""

if [ ! -d /data/toLoad ]; then
        echo "Creating /data/toLoad directory ..."
        mkdir -p /data/toLoad
fi

if [ "$(ls -A /data/toLoad)" ]; then
        echo "/data/toLoad contains initialization data ... skipping copy"
else
        echo "Substituting environment variables and copying initialization data ..."
        for file_path in `find /toLoad -type f`; do
		dir_name=$(dirname $file_path)
		file_name=$(basename $file_path)
                ext=`echo $file_name | cut -d '.' -f2`
		mkdir -p /data${dir_name}
		if [ "$ext" = "nq" ]; then
                	echo "envsubst ${dir_name}/${file_name} --> /data${dir_name}/${file_name}"
                	envsubst < /${dir_name}/${file_name} > /data/${dir_name}/${file_name}
		else
			echo "cp ${dir_name}/${file_name} --> /data${dir_name}/${file_name}"
			cp ${dir_name}/${file_name} /data${dir_name}/${file_name}
		fi
        done
fi

echo ""
echo "Sarting Graph Database ..."

/virtuoso.sh

