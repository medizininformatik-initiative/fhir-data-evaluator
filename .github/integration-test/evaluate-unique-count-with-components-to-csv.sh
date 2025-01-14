#!/bin/bash -e

DOCKER_COMPOSE_FILE=.github/integration-test/$1/docker-compose.yml
export FDE_INPUT_MEASURE=/${PWD}/.github/integration-test/measures/unique-count-with-components-measure.json
export FDE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-unique-count-with-components-to-csv-test

mkdir "$FDE_OUTPUT_DIR"
docker compose -f "$DOCKER_COMPOSE_FILE" run -e TZ="$(cat /etc/timezone)" fhir-data-evaluator

today=$(date +"%Y-%m-%d")
OUTPUT_DIR=$(find "$FDE_OUTPUT_DIR" -type d -name "*$today*" | head -n 1)

./csv-converter.sh "$OUTPUT_DIR"/measure-report.json "$OUTPUT_DIR"

EXPECTED_STRATIFIER_COUNT=2
EXPECTED_UNIQUE_STRATIFIER_COUNT=1

STRATIFIER_COUNT=""
UNIQUE_STRATIFIER_COUNT=""
while IFS=, read -r status_system status_code status_display icd10_system icd10_code icd10_display count unique_count; do
    if [[ $status_system == \"http://terminology.hl7.org/CodeSystem/condition-clinical\" && $status_code == \"active\" && \
          $icd10_system == \"http://fhir.de/CodeSystem/bfarm/icd-10-gm\" && $icd10_code == \"I60.1\" ]]; then
        STRATIFIER_COUNT=$count
        UNIQUE_STRATIFIER_COUNT=$unique_count
        break
    fi
done < "$OUTPUT_DIR"/icd10-code-condition-clinical-status.csv

if [ "$STRATIFIER_COUNT" = "$EXPECTED_STRATIFIER_COUNT" ]; then
  echo "OK 👍: stratifier count ($STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: stratifier ($STRATIFIER_COUNT) != $EXPECTED_STRATIFIER_COUNT"
  exit 1
fi

if [ "$UNIQUE_STRATIFIER_COUNT" = "$EXPECTED_UNIQUE_STRATIFIER_COUNT" ]; then
  echo "OK 👍: unique stratifier count ($UNIQUE_STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: unique stratifier ($UNIQUE_STRATIFIER_COUNT) != $EXPECTED_UNIQUE_STRATIFIER_COUNT"
  exit 1
fi