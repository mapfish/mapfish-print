FROM mapfish_print_builder AS builder

FROM tomcat:9.0.98-jdk17-temurin-jammy AS runner
LABEL maintainer="Camptocamp <info@camptocamp.com>"

RUN --mount=type=cache,target=/var/cache,sharing=locked \
    --mount=type=cache,target=/root/.cache \
  perl -0777 -i -pe 's/(<Valve className="org.apache.catalina.valves.AccessLogValve"[^>]*>)/<Valve className="ch.qos.logback.access.tomcat.LogbackValve" quiet="true"\/>/s' "${CATALINA_HOME}/conf/server.xml" \
  && echo "tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*" >> "${CATALINA_HOME}/conf/catalina.properties" \
  && echo "org.apache.catalina.startup.TldConfig.jarsToSkip=*" >> "${CATALINA_HOME}/conf/catalina.properties" \
  && echo "tomcat.util.scan.DefaultJarScanner.jarsToSkip=*" >> "${CATALINA_HOME}/conf/catalina.properties" \
  && perl -0777 -i -pe 's/<\/Context>/<Resources cachingAllowed="true" cacheMaxSize="102400"\/><\/Context>/' "${CATALINA_HOME}/conf/context.xml" \
  && mkdir --parent /usr/local/tomcat/conf/Catalina /usr/local/tomcat/work/Catalina \
  && chmod -R g+rwx /usr/local/tomcat/conf/Catalina /usr/local/tomcat/work \
  && chgrp -R root /usr/local/tomcat/conf/Catalina /usr/local/tomcat/work \
  && chmod g+r /usr/local/tomcat/conf/* \
  && apt-get update \
  && apt-get upgrade --assume-yes \
  && apt-get install --assume-yes --no-install-recommends curl \
    fonts-liberation fonts-dejavu fontconfig

COPY --from=builder /src/core/build/webapp "${CATALINA_HOME}/webapps/ROOT/"
COPY docker /

RUN mkdir -p "${CATALINA_HOME}/extlib/classes/org/mapfish/print" \
  && cp -r "${CATALINA_HOME}/webapps/ROOT/WEB-INF/classes/org/mapfish/print/url" "${CATALINA_HOME}/extlib/classes/org/mapfish/print/" \
  && perl -0777 -i -pe 's/<Valve className="ch.qos.logback.access.tomcat.LogbackValve" quiet="true"\/>//s' "${CATALINA_HOME}/conf/server.xml" \
  && chmod g+r -R /usr/local/tomcat/conf/ \
  && chmod g+rw /usr/local/tomcat/temp/ /usr/local/tomcat/webapps/ROOT/WEB-INF/lib \
  && chmod g+rw /usr/local/tomcat/webapps/ROOT/WEB-INF/classes /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/*.xml \
  && adduser www-data root

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

COPY extraConfigFor/acceptanceTests/mapfish-spring-application-context-override.xml /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/

FROM runner AS watcher

RUN --mount=type=cache,target=/var/cache,sharing=locked \
    --mount=type=cache,target=/root/.cache \
  apt-get update \
  && apt-get install --yes --no-install-recommends python3-pip rsync python3-setuptools \
  && python3 -m pip --disable-pip-version-check --no-cache-dir install inotify

FROM runner AS final
