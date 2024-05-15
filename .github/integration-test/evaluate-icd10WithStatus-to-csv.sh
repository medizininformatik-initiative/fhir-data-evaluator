#!/bin/bash -e

INPUT_MEASURE=$1
BASE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-icd10WithStatus-to-csv-test
mkdir "$BASE_OUTPUT_DIR"

docker run -v "$INPUT_MEASURE":/app/measure.json -v "$BASE_OUTPUT_DIR":/app/output/ -e FHIR_SERVER=http://fhir-server:8080/fhir \
       -e CONVERT_TO_CSV=true -e TZ="$(cat /etc/timezone)" --network integration-test_testing-network fhir-data-evaluator

today=$(date +"%Y-%m-%d")
OUTPUT_DIR=$(find "$BASE_OUTPUT_DIR" -type d -name "*$today*" | head -n 1)

#wait for csv file creation
sleep 1

EXPECTED_STRATIFIER_COUNT=2

STRATIFIER_COUNT=""
while IFS=, read -r status_system status_code status_display icd10_system icd10_code icd10_display count; do
    if [[ $status_system == \"http://terminology.hl7.org/CodeSystem/condition-clinical\" && $status_code == \"active\" && \
          $icd10_system == \"http://fhir.de/CodeSystem/bfarm/icd-10-gm\" && $icd10_code == \"I60.1\" ]]; then
        STRATIFIER_COUNT=$count
        break
    fi
done < "$OUTPUT_DIR"/icd10-code-and-status.csv

if [ "$STRATIFIER_COUNT" = "$EXPECTED_STRATIFIER_COUNT" ]; then
  echo "OK 👍: stratifier count ($STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: stratifier ($STRATIFIER_COUNT) != $EXPECTED_STRATIFIER_COUNT"
  exit 1
fi
