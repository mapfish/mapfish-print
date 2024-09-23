CLASSPATH=${CATALINA_HOME}/bin/bootstrap.jar:${CATALINA_HOME}/bin/tomcat-juli.jar

for jar in ${CATALINA_HOME}/lib/*; do
    CLASSPATH=${CLASSPATH}:$jar
done
