FROM eclipse-temurin:25.0.5.1_2-jre-noble@sha256:2ec98aa522c472deadfd2b7c5b39bca680e33ef522a53a72f10789bdf89a6bb5

RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y jq && \
    apt-get clean

COPY /target/fhir-data-evaluator.jar /app/

ENV CERTIFICATE_PATH=/app/certs
ENV TRUSTSTORE_PATH=/app/truststore
ENV TRUSTSTORE_FILE=self-signed-truststore.jks

RUN mkdir -p $CERTIFICATE_PATH $TRUSTSTORE_PATH
RUN chown 1001 $CERTIFICATE_PATH $TRUSTSTORE_PATH

WORKDIR /app
USER 1001

COPY ./docker/docker-entrypoint.sh /
ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]
