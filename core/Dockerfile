FROM mapfish_print_builder AS builder

FROM tomcat:11.0.15-jre21-temurin-jammy AS runner
LABEL maintainer="Camptocamp <info@camptocamp.com>"

RUN --mount=type=cache,target=/var/cache,sharing=locked \
    --mount=type=cache,target=/root/.cache \
  apt-get update \
  && apt-get upgrade --assume-yes \
  && apt-get install --assume-yes --no-install-recommends curl \
    fonts-liberation fonts-dejavu fontconfig

RUN perl -0777 -i -pe 's/(<Valve className="org.apache.catalina.valves.AccessLogValve"[^>]*>)/<Valve className="ch.qos.logback.access.tomcat.LogbackValve" \/>/s' "${CATALINA_HOME}/conf/server.xml" \
  && echo "tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*" >> "${CATALINA_HOME}/conf/catalina.properties" \
  && echo "org.apache.catalina.startup.TldConfig.jarsToSkip=*" >> "${CATALINA_HOME}/conf/catalina.properties" \
  && echo "tomcat.util.scan.DefaultJarScanner.jarsToSkip=*" >> "${CATALINA_HOME}/conf/catalina.properties" \
  && perl -0777 -i -pe 's/<\/Context>/<Resources cachingAllowed="true" cacheMaxSize="102400"\/><\/Context>/' "${CATALINA_HOME}/conf/context.xml" \
  && mkdir --parent ${CATALINA_HOME}/conf/Catalina ${CATALINA_HOME}/work/Catalina \
  && chmod -R g+rwx ${CATALINA_HOME}/conf/Catalina ${CATALINA_HOME}/work \
  && chgrp -R root ${CATALINA_HOME}/conf/Catalina ${CATALINA_HOME}/work \
  && chmod g+r ${CATALINA_HOME}/conf/*

COPY --from=builder /src/core/build/webapp "${CATALINA_HOME}/webapps/ROOT/"
COPY docker /

RUN mkdir -p "${CATALINA_HOME}/extlib/classes/org/mapfish/print" \
  && cp -r "${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/org/mapfish/print/url" "${CATALINA_HOME}/extlib/classes/org/mapfish/print/" \
  && chmod g+rx -R ${CATALINA_HOME}/conf/ \
  && chmod g+rwx -R ${CATALINA_HOME}/conf/Catalina/ \
  && chmod g+rw ${CATALINA_HOME}/temp/ ${CATALINA_HOME}/webapps/ROOT/WEB-INF/lib/ \
  && chmod g+rw ${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes ${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/*.xml \
  && adduser www-data root \
  && cp ${CATALINA_HOME}/webapps/ROOT/WEB-INF/lib/logback-*.jar ${CATALINA_HOME}/webapps/ROOT/WEB-INF/lib/logstash-logback-*.jar ${CATALINA_HOME}/lib/

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
  PRINT_YAML_MAX_ALIASES=50 \
  SENTRY_DSN=

CMD ["/usr/local/tomcat/bin/docker-start-print"]

FROM runner AS tester

COPY extraConfigFor/acceptanceTests/mapfish-spring-application-context-override.xml ${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/
COPY extraConfigFor/acceptanceTests/mapfish-spring-application-context-override-db.xml ${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/
RUN chmod go+rw ${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/mapfish-spring-application-context-override*.xml

FROM runner AS watcher

RUN --mount=type=cache,target=/var/cache,sharing=locked \
    --mount=type=cache,target=/root/.cache \
  apt-get update \
  && apt-get install --yes --no-install-recommends python3-pip rsync python3-setuptools \
  && python3 -m pip --disable-pip-version-check --no-cache-dir install inotify

FROM runner AS final
