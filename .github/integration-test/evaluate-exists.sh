#!/bin/bash -e

DOCKER_COMPOSE_FILE=.github/integration-test/$1/docker-compose.yml
export FDE_INPUT_MEASURE=/${PWD}/.github/integration-test/measures/exists-measure.json
export FDE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-exists-test

mkdir "$FDE_OUTPUT_DIR"
docker compose -f "$DOCKER_COMPOSE_FILE" run -e TZ="$(cat /etc/timezone)" fhir-data-evaluator

today=$(date +"%Y-%m-%d")
OUTPUT_DIR=$(find "$FDE_OUTPUT_DIR" -type d -name "*$today*" | head -n 1)
REPORT="$OUTPUT_DIR/measure-report.json"

EXPECTED_POPULATION_COUNT=2

POPULATION_COUNT=$(jq '.group[0].population[0].count' "$REPORT")
if [ "$POPULATION_COUNT" = "$EXPECTED_POPULATION_COUNT" ]; then
  echo "OK 👍: population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: population count ($POPULATION_COUNT) != $EXPECTED_POPULATION_COUNT"
  exit 1
fi

find_stratum_count() {
  local EXPECTED_VALUE="$1"
  local COUNT=0
  COUNT=$(jq -c '.group[0].stratifier[0].stratum[]' "$REPORT" | while IFS= read -r stratum; do
    value=$(echo "$stratum" | jq -r '.value.coding[0].code')
    if [ "$value" = "$EXPECTED_VALUE" ]; then
      local COUNT=0
      COUNT=$(echo "$stratum" | jq '.population[0].count')
      echo "$COUNT"
      break
    fi
  done)
  echo "$COUNT"
}

validate_stratum() {
  local STRATUM_VALUE="$1"
  local STRATUM_COUNT="$2"
  local EXPECTED_STRATUM_COUNT="$3"

  if [ "$STRATUM_COUNT" = "$EXPECTED_STRATUM_COUNT" ]; then
    echo "OK 👍: stratum count ($STRATUM_COUNT) equals the expected count for (value=$STRATUM_VALUE)"
  else
    echo "Fail 😞: stratum count ($STRATUM_COUNT) != $EXPECTED_STRATUM_COUNT for (value=$STRATUM_VALUE)"
    exit 1
  fi
}


EXPECTED_STRATUM_COUNT_1=1
EXPECTED_STRATUM_COUNT_2=1
EXPECTED_STRATUM_VALUE_1="true"
EXPECTED_STRATUM_VALUE_2="false"

STRATUM_COUNT_1=$(find_stratum_count $EXPECTED_STRATUM_VALUE_1)
STRATUM_COUNT_2=$(find_stratum_count $EXPECTED_STRATUM_VALUE_2)

validate_stratum "$EXPECTED_STRATUM_VALUE_1" "$STRATUM_COUNT_1" "$EXPECTED_STRATUM_COUNT_1"
validate_stratum "$EXPECTED_STRATUM_VALUE_2" "$STRATUM_COUNT_2" "$EXPECTED_STRATUM_COUNT_2"
