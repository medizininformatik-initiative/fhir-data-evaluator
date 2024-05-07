FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y jq && \
    apt-get clean

COPY /target/fhir-data-evaluator.jar /app/
COPY /src/main/csv-converter.sh /app/csv-converter.sh

WORKDIR /app
USER 1001

COPY ./docker/docker-entrypoint.sh /
ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]
