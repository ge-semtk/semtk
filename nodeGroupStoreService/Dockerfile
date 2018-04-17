FROM java:8

MAINTAINER Alex Iankoulski <iankouls@ge.com>

ARG http_proxy
ARG https_proxy

RUN [ -n "$http_proxy" ] && echo "Acquire::http::proxy \"${http_proxy}\";" > /etc/apt/apt.conf; \
    [ -n "$https_proxy" ] && echo "Acquire::https::proxy \"${https_proxy}\";" >> /etc/apt/apt.conf; \
    [ -f /etc/apt/apt.conf ] && cat /etc/apt/apt.conf; exit 0

RUN apt-get update && apt-get install -y net-tools

COPY target/*.jar /service/

COPY service/*.properties /service/

CMD cd /service; ls -al; java ${JVM_OPTIONS} -jar `ls -tr *.jar | tail -n1` --spring.config.location=${CONFIG_NODEGROUPSTORE_SERVICE} --multipart.maxFileSize=1000Mb

