#!/bin/sh

otilmHome="/opt/otilm"
source ${otilmHome}/static-functions

#if [ -f ${otilmHome}/trusted-certificates.pem ]
#then
#  log "INFO" "Adding additional trusted certificates to cacerts"
#  ./update-cacerts.sh /opt/otilm/trusted-certificates.pem
#else
#  log "INFO" "No trusted certificates were provided, continue!"
#fi

log "INFO" "Launching the Core"

# Set debug options if required
if [ x"${REMOTE_DEBUG}" != x ] && [ "${REMOTE_DEBUG}" != "false" ]
then
  java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar ./app.jar
else
  java -jar ./app.jar
fi

#exec "$@"