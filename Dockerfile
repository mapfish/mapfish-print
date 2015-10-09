#
# Dockerfile building and starting an mapfish-print server.
#
# HowTo use this file?
# - Clone https://github.com/mapfish/mapfish-print to the computer which is running docker.
# - Checkout the Git-Tag you desire. This script was tested with release/3.1.2
# - In order to build an image run:
#    docker build --rm --force-rm --tag=$YOURWONDERFULLTAGNAME .
# This might fail because some repositories are currently not available. Try it again.
# The image will build in the end.
# After building the Image you need to run it. Tomcat7 requires some special treatment,
# thats why the --cap-add parameter is important.
# docker run -p $AFREEPORTONYOURDOCKERHOST:8080 --cap-add SYS_PTRACE -ti $YOURWONDERFULTAGNAME

# In case you wonder, how you can add your own print-apps, you need to copy them
# to: /var/lib/tomcat7/webapps/print-servlet-X.X.X/print-apps, but how to get them there?
# run the image by mounting your print-apps as a volume to the container. For example:
# docker run -p $AFREEPORTONYOURDOCKERHOST:8080 -v $ABSPATHTOPRINTAPPS:/mnt --cap-add SYS_PTRACE -ti $YOURWONDERFULTAGNAME
# then you can connect to the container by using:
# docker exec $CONTAINERID -ti /bin/bash
# from a second shell.
# Now you can copy your print-apps to /var/lib/tomcat7/webapps/print-servlet-X.X.X/print-apps
# and restart the service: service tomcat7 restart

FROM debian:latest
MAINTAINER dustin@intevation.de

RUN apt-get update && \
    apt-get -y install tomcat7 openjdk-7-jdk

RUN mkdir /opt/mapfish-print/
ADD . /opt/mapfish-print/

RUN cd /opt/mapfish-print/ && \
    ./gradlew build

RUN cp /opt/mapfish-print/core/build/libs/print-servlet-3.1.2.war \
       /var/lib/tomcat7/webapps/

EXPOSE 8080

CMD /etc/init.d/tomcat7 start && \
    tail -f /var/log/tomcat7/catalina.out
