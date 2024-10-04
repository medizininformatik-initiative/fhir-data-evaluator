#!/bin/bash -e

DOCKER_COMPOSE_FILE=.github/integration-test/$1/docker-compose.yml
export FDE_INPUT_MEASURE=/${PWD}/.github/integration-test/measures/icd10-measure.json
export FDE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-icd10-to-csv-test
export FDE_CONVERT_TO_CSV=true

mkdir "$FDE_OUTPUT_DIR"
docker compose -f "$DOCKER_COMPOSE_FILE" run -e TZ="$(cat /etc/timezone)" fhir-data-evaluator

today=$(date +"%Y-%m-%d")
OUTPUT_DIR=$(find "$FDE_OUTPUT_DIR" -type d -name "*$today*" | head -n 1)

#wait for csv file creation
sleep 1

EXPECTED_STRATIFIER_COUNT=2

STRATIFIER_COUNT=""
while IFS=, read -r system code display count; do
    if [[ $system == \"http://fhir.de/CodeSystem/bfarm/icd-10-gm\" && $code == \"I60.1\" ]]; then
        STRATIFIER_COUNT=$count
        break
    fi
done < "$OUTPUT_DIR"/icd10-code.csv

if [ "$STRATIFIER_COUNT" = "$EXPECTED_STRATIFIER_COUNT" ]; then
  echo "OK 👍: stratifier count ($STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: stratifier ($STRATIFIER_COUNT) != $EXPECTED_STRATIFIER_COUNT"
  exit 1
fi
