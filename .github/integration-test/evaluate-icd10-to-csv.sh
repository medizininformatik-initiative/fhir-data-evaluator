#!/bin/bash

INPUT_MEASURE=$1
CSV_OUTPUT_DIR=$2

docker run -v $INPUT_MEASURE:/app/input-measure.json -v $CSV_OUTPUT_DIR:/app/csv-output/ -e CONVERT_TO_CSV=true -e FHIR_SERVER=http://fhir-server:8080/fhir --network integration-test_testing-network fhir-data-evaluator

#wait for csv file creation
sleep 1

TODAY=$(date +"%Y-%m-%d")
EXPECTED_STRATIFIER_COUNT=2

STRATIFIER_COUNT=""
while IFS=, read -r system code display count; do
    if [[ $system == \"http://fhir.de/CodeSystem/bfarm/icd-10-gm\" && $code == \"I60.1\" ]]; then
        STRATIFIER_COUNT=$count
        break
    fi
done < $CSV_OUTPUT_DIR$TODAY/group0-icd10-code.csv

if [ "$STRATIFIER_COUNT" = "$EXPECTED_STRATIFIER_COUNT" ]; then
  echo "OK 👍: stratifier count ($STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: stratifier ($STRATIFIER_COUNT) != $EXPECTED_STRATIFIER_COUNT"
  exit 1
fi
