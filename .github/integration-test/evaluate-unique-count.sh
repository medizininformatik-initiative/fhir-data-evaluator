#!/bin/bash -e

INPUT_MEASURE=$1
BASE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-unique-count-test
mkdir "$BASE_OUTPUT_DIR"

docker run -v "$INPUT_MEASURE":/app/measure.json -v "$BASE_OUTPUT_DIR":/app/output/ -e FHIR_SERVER=http://fhir-server:8080/fhir \
      -e TZ="$(cat /etc/timezone)" --network integration-test_testing-network fhir-data-evaluator

today=$(date +"%Y-%m-%d")
OUTPUT_DIR=$(find "$BASE_OUTPUT_DIR" -type d -name "*$today*" | head -n 1)
REPORT="$OUTPUT_DIR/measure-report.json"

EXPECTED_INITIAL_POPULATION_COUNT=2
EXPECTED_MEASURE_POPULATION_COUNT=2
EXPECTED_OBSERVATION_POPULATION_COUNT=2
EXPECTED_MEASURE_SCORE=1

# validate group
POPULATION_COUNT=$(jq '.group[0].population[] | select(.code.coding[0].code == "initial-population").count' "$REPORT")
if [ "$POPULATION_COUNT" = "$EXPECTED_INITIAL_POPULATION_COUNT" ]; then
  echo "OK 👍: initial population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: initial population count ($POPULATION_COUNT) != $EXPECTED_INITIAL_POPULATION_COUNT"
  exit 1
fi

POPULATION_COUNT=$(jq '.group[0].population[] | select(.code.coding[0].code == "measure-population").count' "$REPORT")
if [ "$POPULATION_COUNT" = "$EXPECTED_MEASURE_POPULATION_COUNT" ]; then
  echo "OK 👍: measure population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: measure population count ($POPULATION_COUNT) != $EXPECTED_MEASURE_POPULATION_COUNT"
  exit 1
fi

POPULATION_COUNT=$(jq '.group[0].population[] | select(.code.coding[0].code == "measure-observation").count' "$REPORT")
if [ "$POPULATION_COUNT" = "$EXPECTED_OBSERVATION_POPULATION_COUNT" ]; then
  echo "OK 👍: measure observation population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: measure observation population count ($POPULATION_COUNT) != $EXPECTED_OBSERVATION_POPULATION_COUNT"
  exit 1
fi

MEASURE_SCORE=$(jq '.group[0].measureScore.value' "$REPORT")
if [ "$MEASURE_SCORE" = "$EXPECTED_MEASURE_SCORE" ]; then
  echo "OK 👍: group measure score ($MEASURE_SCORE) equals the expected score"
else
  echo "Fail 😞: group measure score ($MEASURE_SCORE) != $EXPECTED_MEASURE_SCORE"
  exit 1
fi

# validate stratifier
POPULATION_COUNT=$(jq '.group[0].stratifier[0].stratum[0].population[] | select(.code.coding[0].code == "initial-population").count' "$REPORT")
if [ "$POPULATION_COUNT" = "$EXPECTED_INITIAL_POPULATION_COUNT" ]; then
  echo "OK 👍: initial population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: initial population count ($POPULATION_COUNT) != $EXPECTED_INITIAL_POPULATION_COUNT"
  exit 1
fi

POPULATION_COUNT=$(jq '.group[0].stratifier[0].stratum[0].population[] | select(.code.coding[0].code == "measure-population").count' "$REPORT")
if [ "$POPULATION_COUNT" = "$EXPECTED_MEASURE_POPULATION_COUNT" ]; then
  echo "OK 👍: measure population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: measure population count ($POPULATION_COUNT) != $EXPECTED_MEASURE_POPULATION_COUNT"
  exit 1
fi

POPULATION_COUNT=$(jq '.group[0].stratifier[0].stratum[0].population[] | select(.code.coding[0].code == "measure-observation").count' "$REPORT")
if [ "$POPULATION_COUNT" = "$EXPECTED_OBSERVATION_POPULATION_COUNT" ]; then
  echo "OK 👍: measure observation population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: measure observation population count ($POPULATION_COUNT) != $EXPECTED_OBSERVATION_POPULATION_COUNT"
  exit 1
fi

MEASURE_SCORE=$(jq '.group[0].stratifier[0].stratum[0].measureScore.value' "$REPORT")
if [ "$MEASURE_SCORE" = "$EXPECTED_MEASURE_SCORE" ]; then
  echo "OK 👍: group measure score ($MEASURE_SCORE) equals the expected score"
else
  echo "Fail 😞: group measure score ($MEASURE_SCORE) != $EXPECTED_MEASURE_SCORE"
  exit 1
fi
