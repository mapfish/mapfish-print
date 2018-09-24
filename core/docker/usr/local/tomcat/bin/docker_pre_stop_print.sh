#!/bin/bash -e
TIMEOUT="${1:-86400}"
echo "Notify the print to stop and wait ${TIMEOUT}s"
cd /usr/local/tomcat/temp/mapfish-print/ROOT
touch stop

x=0
while [ "$x" -lt ${TIMEOUT} -a ! -e stopped ]; do
        x=$((x+1))
        sleep 1
done

if [ -e stopped ]
then
   echo "Print stopped"
   exit 0
else
   echo "Timeout waiting for the print to stop"
   exit 1
fi
