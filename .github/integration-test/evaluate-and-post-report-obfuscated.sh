#!/bin/bash -e

AUTH="$4"
DOCKER_COMPOSE_FILE=.github/integration-test/"$1"/docker-compose.yml
PROJECT_IDENTIFIER_VALUE="$2"
PROJECT_IDENTIFIER_VALUE_OBFUSCATED="$3"
export FDE_INPUT_MEASURE=/${PWD}/.github/integration-test/measures/icd10-measure.json
export FDE_OUTPUT_DIR=$PWD/.github/integration-test/evaluate-and-post-obfuscated
export FDE_AUTHOR_IDENTIFIER_SYSTEM=http://dsf.dev/sid/organization-identifier
export FDE_AUTHOR_IDENTIFIER_VALUE=Test_DIC1
export FDE_PROJECT_IDENTIFIER_SYSTEM=http://medizininformatik-initiative.de/sid/project-identifier
export FDE_PROJECT_IDENTIFIER_VALUE="$PROJECT_IDENTIFIER_VALUE"
export FDE_PROJECT_IDENTIFIER_SYSTEM_OBFUSCATED_REPORT=http://medizininformatik-initiative.de/sid/project-identifier
export FDE_PROJECT_IDENTIFIER_VALUE_OBFUSCATED_REPORT="$PROJECT_IDENTIFIER_VALUE_OBFUSCATED"
export FDE_SEND_REPORT_TO_SERVER=true
export FDE_CREATE_OBFUSCATED_REPORT=true

mkdir "$FDE_OUTPUT_DIR"

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

report_response=$(get_response "MeasureReport")
reference_response=$(get_response "DocumentReference")

EXPECTED_POPULATION_COUNT_RAW=2
EXPECTED_STRATIFIER_COUNT_RAW=2
EXPECTED_POPULATION_COUNT_OBFUSCATED=5
EXPECTED_STRATIFIER_COUNT_OBFUSCATED=5

raw_report_id=$(echo "$reference_response" | jq -r --arg PROJECT_IDENTIFIER_VALUE "$PROJECT_IDENTIFIER_VALUE" '.entry | map(select(.resource.masterIdentifier.value == $PROJECT_IDENTIFIER_VALUE)) | .[0].resource.content[0].attachment.url' | cut -c 15-)
obfuscated_report_id=$(echo "$reference_response" | jq -r --arg PROJECT_IDENTIFIER_VALUE_OBFUSCATED "$PROJECT_IDENTIFIER_VALUE_OBFUSCATED" '.entry | map(select(.resource.masterIdentifier.value == $PROJECT_IDENTIFIER_VALUE_OBFUSCATED)) | .[0].resource.content[0].attachment.url' | cut -c 15-)

raw_report=$(echo "$report_response" | jq -r --arg RAW_REPORT_ID "$raw_report_id" '.entry | map(select(.resource.id == $RAW_REPORT_ID)) | .[0].resource')
obfuscated_report=$(echo "$report_response" | jq -r --arg OBFUSCATED_REPORT_ID "$obfuscated_report_id" '.entry | map(select(.resource.id == $OBFUSCATED_REPORT_ID)) | .[0].resource')


# test raw report
population_count=$(echo "$raw_report" | jq '.group[0].population[0].count')
if [ "$population_count" = "$EXPECTED_POPULATION_COUNT_RAW" ]; then
  echo "OK 👍: raw population count ($population_count) equals the expected count"
else
  echo "Fail 😞: raw population count ($population_count) != ($EXPECTED_POPULATION_COUNT_RAW)"
  exit 1
fi

stratifier_count=$(echo "$raw_report" | jq -r '.group[0].stratifier[0].stratum[0].population[0].count')
if [ "$stratifier_count" = "$EXPECTED_STRATIFIER_COUNT_RAW" ]; then
  echo "OK 👍: raw stratifier count ($stratifier_count) equals the expected count"
else
  echo "Fail 😞: raw stratifier ($stratifier_count) != ($EXPECTED_STRATIFIER_COUNT_RAW)"
  exit 1
fi

# test obfuscated report
population_count=$(echo "$obfuscated_report" | jq '.group[0].population[0].count')
if [ "$population_count" = "$EXPECTED_POPULATION_COUNT_OBFUSCATED" ]; then
  echo "OK 👍: obfuscated population count ($population_count) equals the expected count"
else
  echo "Fail 😞: obfuscated population count ($population_count) != ($EXPECTED_POPULATION_COUNT_OBFUSCATED)"
  exit 1
fi

stratifier_count=$(echo "$obfuscated_report" | jq -r '.group[0].stratifier[0].stratum[0].population[0].count')
if [ "$stratifier_count" = "$EXPECTED_STRATIFIER_COUNT_OBFUSCATED" ]; then
  echo "OK 👍: obfuscated stratifier count ($stratifier_count) equals the expected count"
else
  echo "Fail 😞: obfuscated ($stratifier_count) != ($EXPECTED_STRATIFIER_COUNT_OBFUSCATED)"
  exit 1
fi
