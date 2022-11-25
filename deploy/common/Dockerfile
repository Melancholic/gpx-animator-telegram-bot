FROM bellsoft/liberica-openjdk-alpine:19
VOLUME /tmp
ARG JAR_FILE
ARG ENTRY_POINT
ARG GPX_ANIMATOR_VER
ARG GPX_FILE_FOR_FORECAST
EXPOSE 3000/tcp
ENV LANG en_US.utf8
RUN echo "https://dl-cdn.alpinelinux.org/alpine/v3.12/main" >  /etc/apk/repositories \
    && echo "https://dl-cdn.alpinelinux.org/alpine/v3.12/community" >> /etc/apk/repositories

RUN apk update \
    && apk upgrade --available && sync \
    && apk add ca-certificates libva-intel-driver \
    && apk add --no-cache tzdata \
    && apk add wget \
    && rm -rf /var/cache/

# Required for fonts in java
RUN apk add fontconfig ttf-dejavu \
    && apk add --update font-adobe-100dpi fontconfig ttf-dejavu \
    && fc-cache --force \
    && /usr/glibc/sbin/ldconfig \
    && rm -rf /var/cache/

RUN wget -q https://download.gpx-animator.app/gpx-animator-${GPX_ANIMATOR_VER}-all.jar -O gpx-animator-app.jar  \
    && java -jar gpx-animator-app.jar --version

COPY ${JAR_FILE} gpx-animator-telegram-bot.jar
COPY ${ENTRY_POINT} entry_point.sh
COPY ${GPX_FILE_FOR_FORECAST} gpx_for_forecast.gpx

ENTRYPOINT ["sh", "./entry_point.sh"]
