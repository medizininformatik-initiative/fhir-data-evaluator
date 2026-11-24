FROM eclipse-temurin:17.0.21.1_2-jre@sha256:1c8ecf975c9b8e4f1b574af0489c244fec250c4bb42bf8ea726b3e0c9169b1f1

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
