# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
COPY src /home/app/src
COPY pom.xml /home/app
COPY docker /home/app/docker
COPY ejbca-libs /home/app/ejbca-libs
RUN /home/app/ejbca-libs/maven-install-files.sh
RUN mvn -f /home/app/pom.xml clean package

# Package stage
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.authors="ILM <ilm@omnitrust.com>"

# add non root user ilm
RUN addgroup --system --gid 10001 ilm && adduser --system --home /opt/otilm --uid 10001 --ingroup ilm ilm

COPY --from=build /home/app/docker /
COPY --from=build /home/app/target/*.jar /opt/otilm/app.jar

WORKDIR /opt/otilm

ENV JDBC_URL=
ENV JDBC_USERNAME=
ENV JDBC_PASSWORD=
ENV DB_SCHEMA=ejbca
ENV PORT=8080
ENV TRUSTED_CERTIFICATES=
ENV REMOTE_DEBUG=false

ENV HTTP_PROXY=
ENV HTTPS_PROXY=
ENV NO_PROXY=

USER 10001

ENTRYPOINT ["/opt/otilm/entry.sh"]
