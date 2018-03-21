# Build kdl-sparqlgraph

FROM tomcat:8.0.36

MAINTAINER Justin McHugh <mchugh@ge.com>

COPY iidx-oss/ /usr/local/tomcat/webapps/iidx-oss/
COPY ROOT/ /usr/local/tomcat/webapps/ROOT/
COPY sparqlForm/ /usr/local/tomcat/webapps/sparqlForm/
COPY sparqlGraph/ /usr/local/tomcat/webapps/sparqlGraph/
COPY ./entrypoint.sh /usr/local/tomcat/entrypoint.sh
RUN chmod +x /usr/local/tomcat/*.sh

WORKDIR /usr/local/tomcat

CMD /usr/local/tomcat/entrypoint.sh
