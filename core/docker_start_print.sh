#!/bin/bash
set -e
cd /usr/local/tomcat/webapps/ROOT
PG_LIB=`find WEB-INF/lib -name "postgresql-*"`
if java ${CATALINA_OPTS} -cp WEB-INF/classes/:$PG_LIB org.mapfish.print.WaitDB
then
    cp WEB-INF/classes/mapfish-spring-application-context-override-db.xml \
       WEB-INF/classes/mapfish-spring-application-context-override.xml
fi

catalina.sh run
