#!/bin/bash -e

INPUT_MEASURE=$1
BASE_OUTPUT_DIR=$PWD/.github/integration-test

today=$(date +"%Y-%m-%d_%H-%M-%S")
measureName=$(jq -c --raw-output '.name' "$INPUT_MEASURE")
OUTPUT_DIR=$BASE_OUTPUT_DIR/$today-$measureName

docker run -v "$INPUT_MEASURE":/app/measure.json -v "$BASE_OUTPUT_DIR":/app/output/ -e FHIR_SERVER=http://fhir-server:8080/fhir \
      --network integration-test_testing-network fhir-data-evaluator
REPORT=$(cat "$OUTPUT_DIR"/measure-report.json)

EXPECTED_POPULATION_COUNT=2
EXPECTED_STRATIFIER_COUNT=2

POPULATION_COUNT=$(echo "$REPORT" | jq '.group[0].population[0].count')
if [ "$POPULATION_COUNT" = "$EXPECTED_POPULATION_COUNT" ]; then
  echo "OK 👍: population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: population count ($POPULATION_COUNT) != $EXPECTED_POPULATION_COUNT"
  exit 1
fi

STRATIFIER_COUNT=$(echo "$REPORT" | jq -r '.group[0].stratifier[0].stratum[0].population[0].count')
if [ "$STRATIFIER_COUNT" = "$EXPECTED_STRATIFIER_COUNT" ]; then
  echo "OK 👍: stratifier count ($STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: stratifier ($STRATIFIER_COUNT) != $EXPECTED_STRATIFIER_COUNT"
  exit 1
fi
