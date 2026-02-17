#!/bin/bash -e

AUTH="$3"
DOCKER_COMPOSE_FILE=.github/integration-test/"$1"/docker-compose.yml
PROJECT_IDENTIFIER_VALUE="$2"
export FDE_INPUT_MEASURE=/${PWD}/.github/integration-test/measures/icd10-measure.json
export FDE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-and-post
export FDE_AUTHOR_IDENTIFIER_SYSTEM=http://dsf.dev/sid/organization-identifier
export FDE_AUTHOR_IDENTIFIER_VALUE=Test_DIC1
export FDE_PROJECT_IDENTIFIER_SYSTEM=http://medizininformatik-initiative.de/sid/project-identifier
export FDE_PROJECT_IDENTIFIER_VALUE="$PROJECT_IDENTIFIER_VALUE"
export FDE_SEND_REPORT_TO_SERVER=true

if [[ "$1" == *"hapi"* ]]; then
  # Hapi might need some time until the previously created resources are available (the FDE fetches them to get the ID
  # of the previous document reference)
  sleep 60
fi

docker compose -f "$DOCKER_COMPOSE_FILE" run -e TZ="$(cat /etc/timezone)" fhir-data-evaluator

if [[ "$1" == *"hapi"* ]]; then
  sleep 60 # hapi might need some time until the newly updated resources are available
fi

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

reference_response=$(get_response "DocumentReference")

reference_count=$(echo "$reference_response" | jq -r '.entry | length')

EXPECTED_REFERENCE_COUNT=2
if [ "$reference_count" = "$EXPECTED_REFERENCE_COUNT" ]; then
  echo "OK 👍: reference count ($reference_count) equals the expected count"
else
  echo "Fail 😞: reference count ($reference_count) != ($EXPECTED_REFERENCE_COUNT)"
  exit 1
fi
