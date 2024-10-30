#!/bin/bash -e

PROJECT_IDENTIFIER_VALUE="$1"

DOCKER_COMPOSE_FILE=.github/integration-test/no-auth/docker-compose.yml
export FDE_INPUT_MEASURE=/${PWD}/.github/integration-test/measures/icd10-measure.json
export FDE_CONVERT_TO_CSV=false
export FDE_FHIR_REPORT_DESTINATION_SERVER=http://fhir-server:8080/fhir
export FDE_AUTHOR_IDENTIFIER_SYSTEM=http://dsf.dev/sid/organization-identifier
export FDE_AUTHOR_IDENTIFIER_VALUE=Test_DIC1
export FDE_PROJECT_IDENTIFIER_SYSTEM=http://medizininformatik-initiative.de/sid/project-identifier
export FDE_PROJECT_IDENTIFIER_VALUE="$PROJECT_IDENTIFIER_VALUE"
export FDE_SEND_REPORT_TO_SERVER=true

docker compose -f "$DOCKER_COMPOSE_FILE" run -e TZ="$(cat /etc/timezone)" fhir-data-evaluator

reference_response=$(curl -s "http://localhost:8082/fhir/DocumentReference" \
    -H "Content-Type: application/fhir+json")

reference_count=$(echo "$reference_response" | jq -r '.entry | length')

EXPECTED_REFERENCE_COUNT=2
if [ "$reference_count" = "$EXPECTED_REFERENCE_COUNT" ]; then
  echo "OK 👍: reference count ($reference_count) equals the expected count"
else
  echo "Fail 😞: reference count ($reference_count) != ($EXPECTED_REFERENCE_COUNT)"
  exit 1
fi
