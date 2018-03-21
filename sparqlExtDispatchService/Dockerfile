FROM java:8

MAINTAINER Alex Iankoulski <iankouls@ge.com>

RUN apt-get update && apt-get install -y net-tools

COPY target/*.jar /service/

COPY service/*.properties /service/

CMD cd /service; ls -al; java ${JVM_OPTIONS} -Dloader.path="${LOCATION_ADDITIONAL_DISPATCHER_JARS}" -jar `ls -tr *.jar | tail -n1` --spring.config.location=${CONFIG_DISPATCH_SERVICE}

