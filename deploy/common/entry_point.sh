#!/bin/sh
# exec so the JVM becomes PID 1 and receives SIGTERM from `docker stop`,
# giving Spring a graceful shutdown instead of a SIGKILL after the grace period.
exec java \
  -Xmx128m \
  -Xss512k \
  -XX:MetaspaceSize=100m \
  -Dfile.encoding=UTF-8 \
  -jar ./gpx-animator-telegram-bot.jar
