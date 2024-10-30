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

report_response=$(curl -s "http://localhost:8082/fhir/MeasureReport" \
     -H "Content-Type: application/fhir+json")

reference_response=$(curl -s "http://localhost:8082/fhir/DocumentReference" \
    -H "Content-Type: application/fhir+json")

report_url=MeasureReport/$(echo "$report_response" | jq -r  '.entry[0].resource.id')
reference_url=$(echo "$reference_response" | jq -r '.entry[0].resource.content[0].attachment.url')

if [ "$report_url" = "$reference_url" ]; then
  echo "OK 👍: Id of MeasureReport is the same as the referenced attachment in the DocumentReference"
else
  echo "Fail 😞: Id of MeasureReport ($report_url) is not the same as the referenced attachment in the DocumentReference ($reference_url)"
  exit 1
fi

REPORT=$(echo "$report_response" | jq '.entry[0].resource')
EXPECTED_POPULATION_COUNT=2
EXPECTED_STRATIFIER_COUNT=2

POPULATION_COUNT=$(echo "$REPORT" | jq '.group[0].population[0].count')
if [ "$POPULATION_COUNT" = "$EXPECTED_POPULATION_COUNT" ]; then
  echo "OK 👍: population count ($POPULATION_COUNT) equals the expected count"
else
  echo "Fail 😞: population count ($POPULATION_COUNT) != ($EXPECTED_POPULATION_COUNT)"
  exit 1
fi

STRATIFIER_COUNT=$(echo "$REPORT" | jq -r '.group[0].stratifier[0].stratum[0].population[0].count')
if [ "$STRATIFIER_COUNT" = "$EXPECTED_STRATIFIER_COUNT" ]; then
  echo "OK 👍: stratifier count ($STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: stratifier ($STRATIFIER_COUNT) != ($EXPECTED_STRATIFIER_COUNT)"
  exit 1
fi
