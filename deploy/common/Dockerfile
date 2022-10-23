FROM bellsoft/liberica-openjdk-alpine:17
VOLUME /tmp
ARG JAR_FILE
ARG ENTRY_POINT
ARG GPX_ANIMATOR_VER
EXPOSE 3000/tcp

RUN apk upgrade -U \
    && apk add ca-certificates ffmpeg libva-intel-driver \
    && apk add --no-cache tzdata \
    && apk add wget \
    && rm -rf /var/cache/

ENV LANG en_US.utf8

RUN wget https://download.gpx-animator.app/gpx-animator-${GPX_ANIMATOR_VER}-all.jar -O gpx-animator-app.jar  \
    && java -jar gpx-animator-app.jar --help

COPY ${JAR_FILE} gpx-animator-telegram-bot.jar
COPY ${ENTRY_POINT} entry_point.sh

ENTRYPOINT ["sh", "./entry_point.sh"]