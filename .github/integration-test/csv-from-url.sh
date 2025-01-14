#!/bin/bash -e

# This test retrieves the MeasureReport that was uploaded in the `evaluate-and-post-report.sh` test and therefore must
# be executed afterwards.

AUTH="$1"
OUTPUT_DIR=/app/output

FHIR_USER="test"
FHIR_PW="bar"
CLIENT_ID="account"
CLIENT_SECRET="test"
ISSUER_URL="https://secure-keycloak:8443/realms/test/protocol/openid-connect/token"

run_csv_converter() {
  if [ "$AUTH" == "no-auth" ]; then
    doc_ref=$(curl -s 'http://fhir-server:8080/fhir/DocumentReference' -H 'Content-Type: application/fhir+json')
    report_id=$(echo "$doc_ref" | jq -r '.entry[0].resource.content[0].attachment.url')

    bash ./app/csv-converter.sh "http://fhir-server:8080/fhir/$report_id" "$OUTPUT_DIR"
  elif [ "$AUTH" == "basic-auth" ]; then
    doc_ref=$(curl -s 'http://proxy:8080/fhir/DocumentReference' \
                    -H 'Content-Type: application/fhir+json' \
                    -u "$FHIR_USER:$FHIR_PW")
    report_id=$(echo "$doc_ref" | jq -r '.entry[0].resource.content[0].attachment.url')

    bash ./app/csv-converter.sh "http://proxy:8080/fhir/$report_id" "$OUTPUT_DIR" -u "$FHIR_USER" -p "$FHIR_PW"
  else
    cp /app/certs/cert.pem /usr/local/share/ca-certificates/cert.crt && update-ca-certificates
    oauth_response=$(curl -v -X POST "$ISSUER_URL" \
                          -H 'Content-Type: application/x-www-form-urlencoded' \
                          -d 'grant_type=client_credentials' \
                          -d "client_id=$CLIENT_ID" \
                          -d "client_secret=$CLIENT_SECRET")
      echo "oauth response: "
      echo "$oauth_response"
      fhir_report_bearer_token=$(echo "$oauth_response" | jq -r '.access_token')
      echo "bearer token: $fhir_report_bearer_token"
      doc_ref=$(curl -s 'https://secure-fhir-server:8443/fhir/DocumentReference' \
                             -H 'Content-Type: application/fhir+json' \
                             -H "Authorization: Bearer $fhir_report_bearer_token")
      report_id=$(echo "$doc_ref" | jq -r '.entry[0].resource.content[0].attachment.url')
      echo "doc ref: "
      echo "$doc_ref"
      echo "report_id"
      echo "$report_id"
      bash /app/csv-converter.sh "https://secure-fhir-server:8443/fhir/$report_id" "$OUTPUT_DIR" -i "$ISSUER_URL" -c "$CLIENT_ID" -s "$CLIENT_SECRET"
  fi
}
run_csv_converter

EXPECTED_STRATIFIER_COUNT=2

STRATIFIER_COUNT=""
while IFS=, read -r system code display count; do
    if [[ $system == \"http://fhir.de/CodeSystem/bfarm/icd-10-gm\" && $code == \"I60.1\" ]]; then
        STRATIFIER_COUNT=$count
        break
    fi
done < "$OUTPUT_DIR"/icd10-code.csv

if [ "$STRATIFIER_COUNT" = "$EXPECTED_STRATIFIER_COUNT" ]; then
  echo "OK 👍: stratifier count ($STRATIFIER_COUNT) equals the expected count"
else
  echo "Fail 😞: stratifier ($STRATIFIER_COUNT) != $EXPECTED_STRATIFIER_COUNT"
  exit 1
fi
