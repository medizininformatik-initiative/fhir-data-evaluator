#!/bin/bash -e

AUTH="$1"
DOCKER_COMPOSE_FILE=.github/integration-test/"$1"/docker-compose.yml
PROJECT_IDENTIFIER_VALUE="$2"
export FDE_INPUT_MEASURE=/${PWD}/.github/integration-test/measures/icd10-measure.json
export FDE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-and-post
export FDE_AUTHOR_IDENTIFIER_SYSTEM=http://dsf.dev/sid/organization-identifier
export FDE_AUTHOR_IDENTIFIER_VALUE=Test_DIC1
export FDE_PROJECT_IDENTIFIER_SYSTEM=http://medizininformatik-initiative.de/sid/project-identifier
export FDE_PROJECT_IDENTIFIER_VALUE="$PROJECT_IDENTIFIER_VALUE"
export FDE_SEND_REPORT_TO_SERVER=true

mkdir "$FDE_OUTPUT_DIR"
docker compose -f "$DOCKER_COMPOSE_FILE" run -e TZ="$(cat /etc/timezone)" fhir-data-evaluator

get_response() {
  URL="$1"
  CURL_TESTER_ID=$(docker ps --filter "name=$AUTH-curl-tester-1" --format "{{.ID}}")
  if [ "$AUTH" == "no-auth" ]; then
    response=$(docker exec "$CURL_TESTER_ID" sh -c "
      curl -s 'http://fhir-server:8080/fhir/$URL' -H 'Content-Type: application/fhir+json'")
    echo "$response"
  elif [ "$AUTH" == "basic-auth" ]; then
    response=$(docker exec "$CURL_TESTER_ID" sh -c "
          curl -s 'http://proxy:8080/fhir/$URL' \
                -H 'Content-Type: application/fhir+json' \
                -u 'test:bar'")
    echo "$response"
  else
    docker exec "$CURL_TESTER_ID" sh -c "
      cp /app/certs/cert.pem /usr/local/share/ca-certificates/cert.crt &&
      update-ca-certificates"

    oauth_response=$(docker exec "$CURL_TESTER_ID" sh -c "
              curl -s -X POST 'https://secure-keycloak:8443/realms/test/protocol/openid-connect/token' \
                      -H 'Content-Type: application/x-www-form-urlencoded' \
                      -d 'grant_type=client_credentials' \
                      -d 'client_id=account' \
                      -d 'client_secret=test'")
    FHIR_REPORT_BEARER_TOKEN=$(echo "$oauth_response" | jq -r '.access_token')

    response=$(docker exec "$CURL_TESTER_ID" sh -c "
                  curl -s 'https://secure-fhir-server:8443/fhir/$URL' \
                           -H 'Content-Type: application/fhir+json' \
                           -H 'Authorization: Bearer $FHIR_REPORT_BEARER_TOKEN'")

    echo "$response"
  fi
}

report_response=$(get_response "MeasureReport")

reference_response=$(get_response "DocumentReference")

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
