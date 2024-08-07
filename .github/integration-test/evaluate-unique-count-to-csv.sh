#!/bin/bash -e

INPUT_MEASURE=$1
BASE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-unique-count-with-components-test
mkdir "$BASE_OUTPUT_DIR"

docker run -v "$INPUT_MEASURE":/app/measure.json -v "$BASE_OUTPUT_DIR":/app/output/ -e FHIR_SERVER=http://fhir-server:8080/fhir \
      -e TZ="$(cat /etc/timezone)" --network integration-test_testing-network -e CONVERT_TO_CSV=true fhir-data-evaluator

today=$(date +"%Y-%m-%d")
OUTPUT_DIR=$(find "$BASE_OUTPUT_DIR" -type d -name "*$today*" | head -n 1)

#wait for csv file creation
sleep 1

EXPECTED_STRATIFIER_COUNT=2
EXPECTED_UNIQUE_STRATIFIER_COUNT=1

STRATIFIER_COUNT=""
UNIQUE_STRATIFIER_COUNT=""
while IFS=, read -r system code display count unique_count; do
    if [[ $system == \"http://fhir.de/CodeSystem/bfarm/icd-10-gm\" && $code == \"I60.1\" ]]; then
        STRATIFIER_COUNT=$count
        UNIQUE_STRATIFIER_COUNT=$unique_count
        break
    fi
done < "$OUTPUT_DIR"/icd10-code.csv

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