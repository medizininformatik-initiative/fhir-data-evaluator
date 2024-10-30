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

reference_response=$(curl -s "http://localhost:8082/fhir/DocumentReference" \
    -H "Content-Type: application/fhir+json")
reference_url_before=$(echo "$reference_response" | jq -r '.entry[0].resource.content[0].attachment.url')

docker compose -f "$DOCKER_COMPOSE_FILE" run -e TZ="$(cat /etc/timezone)" fhir-data-evaluator

report_response=$(curl -s "http://localhost:8082/fhir/MeasureReport" \
     -H "Content-Type: application/fhir+json")
reference_response=$(curl -s "http://localhost:8082/fhir/DocumentReference" \
    -H "Content-Type: application/fhir+json")

report_count=$(echo "$report_response" | jq -r  '.entry | length')
reference_count=$(echo "$reference_response" | jq -r '.entry | length')
reference_url_after=$(echo "$reference_response" | jq -r '.entry[0].resource.content[0].attachment.url')

EXPECTED_REPORT_COUNT=2
if [ "$report_count" = "$EXPECTED_REPORT_COUNT" ]; then
  echo "OK 👍: report count ($report_count) equals the expected count"
else
  echo "Fail 😞: report count ($report_count) != ($EXPECTED_REPORT_COUNT)"
  exit 1
fi

EXPECTED_REFERENCE_COUNT=1
if [ "$reference_count" = "$EXPECTED_REFERENCE_COUNT" ]; then
  echo "OK 👍: reference count ($reference_count) equals the expected count"
else
  echo "Fail 😞: reference count ($reference_count) != ($EXPECTED_REFERENCE_COUNT)"
  exit 1
fi

if [ "$reference_url_before" != "$reference_url_after" ]; then
  echo "OK 👍: referenced measure report url changed"
else
  echo "Fail 😞: referenced measure report url did not change"
  exit 1
fi
