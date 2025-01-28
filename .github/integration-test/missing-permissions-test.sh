#!/bin/bash -e

BASE_OUTPUT_DIR=$PWD/.github/integration-test/missing-permissions-test
mkdir "$BASE_OUTPUT_DIR"

# Allow docker to exit with an error
set +e
OUTPUT=$(docker run -v "$PWD"/.github/integration-test/measures/code-measure.json:/app/measure.json \
      -v "$BASE_OUTPUT_DIR":/app/output:ro -e FHIR_SOURCE_SERVER=http://fhir-server:8080/fhir fhir-data-evaluator 2>&1)
EXIT_CODE=$?
set -e

if [ "$OUTPUT" = "Missing writing permissions on output directory" ] && [ $EXIT_CODE = 1 ]; then
  echo "OK 👍: docker container exited with code 1 and with the correct error message"
else
  echo "Fail 😞: docker container did not exit with code 1 or with a different error message"
  exit 1
fi
