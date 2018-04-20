FROM java:8

MAINTAINER Alex Iankoulski <iankouls@ge.com>

RUN apt-get update && apt-get install -y net-tools

COPY target/*.jar /service/

COPY service/*.properties /service/

CMD cd /service; ls -al; java ${JVM_OPTIONS} -jar `ls -tr *.jar | tail -n1` --spring.config.location=${CONFIG_ONTOLOGYINFO_SERVICE}

