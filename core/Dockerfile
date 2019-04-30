FROM camptocamp/tomcat-logback:8.5-jre8
LABEL maintainer="info@camptocamp.com"

COPY build/webapp ${CATALINA_HOME}/webapps/ROOT
COPY docker /

RUN rm ${CATALINA_HOME}/webapps/ROOT/WEB-INF/lib/logback-*.jar ${CATALINA_HOME}/webapps/ROOT/WEB-INF/lib/slf4j-*.jar && \
    mkdir -p ${CATALINA_HOME}/extlib/classes/org/mapfish/print && \
    cp -r ${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/org/mapfish/print/url ${CATALINA_HOME}/extlib/classes/org/mapfish/print/ && \
    perl -0777 -i -pe 's/<Valve className="ch.qos.logback.access.tomcat.LogbackValve" quiet="true"\/>//s' ${CATALINA_HOME}/conf/server.xml && \
    chmod g+r -R /usr/local/tomcat/conf/ && \
    chmod g+rw /usr/local/tomcat/temp/ /usr/local/tomcat/webapps/ROOT/WEB-INF/lib && \
    chmod g+rw /usr/local/tomcat/webapps/ROOT/WEB-INF/classes /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/*.xml && \
    adduser www-data root

ENV LOG_LEVEL=INFO \
    SPRING_LOG_LEVEL=WARN \
    JASPER_LOG_LEVEL=WARN \
    APACHE_LOG_LEVEL=WARN \
    SQL_LOG_LEVEL=WARN

CMD ["/usr/local/tomcat/bin/docker_start_print.sh"]
