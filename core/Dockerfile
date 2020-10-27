FROM mapfish_print_builder AS builder


FROM camptocamp/tomcat-logback:9.0-jre11 AS runner
LABEL maintainer="info@camptocamp.com"

RUN \
  apt-get update && \
  apt-get install --assume-yes --no-install-recommends curl \
  fonts-liberation fonts-dejavu fontconfig && \
  apt-get clean && \
  rm --recursive --force /var/lib/apt/lists/*

COPY --from=builder /src/core/build/webapp ${CATALINA_HOME}/webapps/ROOT/
COPY --from=builder /src/examples/build/classes/java/test/org/mapfish/print/HumanAlphaSerie.class ${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/org/mapfish/print/
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
  SQL_LOG_LEVEL=WARN \
  CATALINA_OPTS= \
  DEFAULT_LOG_LEVEL=INFO \
  TOMCAT_LOG_LEVEL=INFO \
  SENTRY_LOG_LEVEL=ERROR \
  SENTRY_REPORTING_LOG_LEVEL=WARN \
  TOMCAT_LOG_TYPE=classic \
  EXTRA_JARS= \
  PRINT_YAML_MAX_ALIASES=50

CMD ["/usr/local/tomcat/bin/docker-start-print"]

FROM runner AS tester

COPY jettyRunExtraFiles/mapfish-spring-application-context-override-acceptencetests.xml \
  webapps/ROOT/WEB-INF/classes/mapfish-spring-application-context-override.xml

FROM runner AS final
