#!/bin/bash -e

INPUT_MEASURE=$1
BASE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-multiple-stratifiers
mkdir "$BASE_OUTPUT_DIR"

docker run -v "$INPUT_MEASURE":/app/measure.json -v "$BASE_OUTPUT_DIR":/app/output/ -e FHIR_SERVER=http://fhir-server:8080/fhir \
      -e TZ="$(cat /etc/timezone)" --network integration-test_testing-network fhir-data-evaluator

today=$(date +"%Y-%m-%d")
OUTPUT_DIR=$(find "$BASE_OUTPUT_DIR" -type d -name "*$today*" | head -n 1)
REPORT="$OUTPUT_DIR/measure-report.json"

find_stratum() {
  local STRATIFIER_INDEX="$1"
  local EXPECTED_VALUE="$2"
  local STRATUM=$(jq -c --arg EXPECTED_VALUE "$EXPECTED_VALUE" --argjson STRATIFIER_INDEX "$STRATIFIER_INDEX" '
              .group[0].stratifier[$STRATIFIER_INDEX].stratum[] | select(.value.coding[0].code == $EXPECTED_VALUE)' "$REPORT" )
  echo "$STRATUM"
}

validate_stratum() {
  local STRATUM="$1"
  local STRATUM_VALUE="$2"
  local EXPECTED_INITIAL_POP_COUNT="$3"
  local EXPECTED_MEASURE_POP_COUNT="$4"
  local EXPECTED_OBSERVATION_POP_COUNT="$5"
  local EXPECTED_MEASURE_SCORE="$6"

  local INITIAL_POP_COUNT=$(jq '.population[] | select(.code.coding[0].code == "initial-population").count' <<< "$STRATUM")
  local MEASURE_POP_COUNT=$(jq '.population[] | select(.code.coding[0].code == "measure-population").count' <<< "$STRATUM")
  local OBSERVATION_POP_COUNT=$(jq '.population[] | select(.code.coding[0].code == "measure-observation").count' <<< "$STRATUM")
  local MEASURE_SCORE=$(jq '.measureScore.value' <<< "$STRATUM")



  if [ "$INITIAL_POP_COUNT" = "$EXPECTED_INITIAL_POP_COUNT" ]; then
    echo "OK 👍: initial population count ($INITIAL_POP_COUNT) equals the expected count for (value=$STRATUM_VALUE)"
  else
    echo "Fail 😞: stratum count ($INITIAL_POP_COUNT) != $EXPECTED_INITIAL_POP_COUNT for initial population (value=$STRATUM_VALUE)"
    exit 1
  fi

  if [ "$MEASURE_POP_COUNT" = "$EXPECTED_MEASURE_POP_COUNT" ]; then
      echo "OK 👍: measure population count ($MEASURE_POP_COUNT) equals the expected count for (value=$STRATUM_VALUE)"
    else
      echo "Fail 😞: stratum count ($MEASURE_POP_COUNT) != $EXPECTED_MEASURE_POP_COUNT for measure population (value=$STRATUM_VALUE)"
      exit 1
  fi

if [ "$OBSERVATION_POP_COUNT" = "$EXPECTED_OBSERVATION_POP_COUNT" ]; then
    echo "OK 👍: observation population count ($OBSERVATION_POP_COUNT) equals the expected count for (value=$STRATUM_VALUE)"
  else
    echo "Fail 😞: stratum count ($OBSERVATION_POP_COUNT) != $EXPECTED_OBSERVATION_POP_COUNT for measure observation (value=$STRATUM_VALUE)"
    exit 1
  fi

  if [ "$MEASURE_SCORE" = "$EXPECTED_MEASURE_SCORE" ]; then
      echo "OK 👍: measure score ($MEASURE_SCORE) equals the expected count for (value=$STRATUM_VALUE)"
    else
      echo "Fail 😞: stratum count ($MEASURE_SCORE) != $EXPECTED_MEASURE_SCORE for measure score (value=$STRATUM_VALUE)"
      exit 1
  fi
}

get_stratum() {
  local STRATIFIER_INDEX="$1"
  local STRATUM_INDEX="$2"
  echo $(jq --argjson STRATIFIER_INDEX "$STRATIFIER_INDEX" --argjson STRATUM_INDEX "STRATUM_INDEX" '.population[]
          | select(.code.coding[0].code == "initial-population").count' "$STRATUM")
}


EXPECTED_STRATUM_VALUE_1="I48.0"
EXPECTED_STRATUM_INITIAL_POP_1=108
EXPECTED_STRATUM_MEASURE_POP_1=108
EXPECTED_STRATUM_OBESERVATION_POP_1=108
EXPECTED_STRATUM_MEASURE_SCORE_1=108

EXPECTED_STRATUM_VALUE_2="fail-no-value-found"
EXPECTED_STRATUM_INITIAL_POP_2=12072
EXPECTED_STRATUM_MEASURE_POP_2=12072
EXPECTED_STRATUM_OBESERVATION_POP_2=12072
EXPECTED_STRATUM_MEASURE_SCORE_2=12040


STRATUM=$(find_stratum 0 $EXPECTED_STRATUM_VALUE_1)
validate_stratum "$STRATUM" $EXPECTED_STRATUM_VALUE_1 $EXPECTED_STRATUM_INITIAL_POP_1 $EXPECTED_STRATUM_MEASURE_POP_1 \
                 $EXPECTED_STRATUM_OBESERVATION_POP_1 $EXPECTED_STRATUM_MEASURE_SCORE_1

STRATUM=$(find_stratum 1 $EXPECTED_STRATUM_VALUE_2)
validate_stratum "$STRATUM" $EXPECTED_STRATUM_VALUE_2 $EXPECTED_STRATUM_INITIAL_POP_2 $EXPECTED_STRATUM_MEASURE_POP_2 \
                 $EXPECTED_STRATUM_OBESERVATION_POP_2 $EXPECTED_STRATUM_MEASURE_SCORE_2
