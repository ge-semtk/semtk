# Build kdl-sparqlgraph

FROM tomcat:8.0.36

MAINTAINER Alex Iankoulski <iankouls@bhge.com>

ARG http_proxy
ARG https_proxy

RUN [ -n "$http_proxy" ] && echo "Acquire::http::proxy \"${http_proxy}\";" > /etc/apt/apt.conf; \
    [ -n "$https_proxy" ] && echo "Acquire::https::proxy \"${https_proxy}\";" >> /etc/apt/apt.conf; \
    [ -f /etc/apt/apt.conf ] && cat /etc/apt/apt.conf; exit 0

COPY iidx-oss/ /usr/local/tomcat/webapps/iidx-oss/
COPY ROOT/ /usr/local/tomcat/webapps/ROOT/
COPY sparqlForm/ /usr/local/tomcat/webapps/sparqlForm/
COPY sparqlGraph/ /usr/local/tomcat/webapps/sparqlGraph/
COPY ./entrypoint.sh /usr/local/tomcat/entrypoint.sh
RUN chmod +x /usr/local/tomcat/*.sh

WORKDIR /usr/local/tomcat

CMD /usr/local/tomcat/entrypoint.sh
