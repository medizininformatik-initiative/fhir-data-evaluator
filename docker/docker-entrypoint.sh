#!/bin/bash -e

if [ "${SEND_REPORT_TO_SERVER}" = true ]; then
  vars_for_upload=(FHIR_REPORT_SERVER AUTHOR_IDENTIFIER_SYSTEM AUTHOR_IDENTIFIER_VALUE PROJECT_IDENTIFIER_SYSTEM PROJECT_IDENTIFIER_VALUE)
  for var in "${vars_for_upload[@]}"; do
     if [[ -z "${!var}" ]]; then
        echo "In order to upload the MeasureReport to a FHIR server, all following environment variables must be set" \
              "(but currently are not set): ${vars_for_upload[*]}"
        exit 1
     fi
  done
else
  if [ ! -w /app/output ]; then
      echo "Missing writing permissions on output directory" >&2
      exit 1
  fi
fi

now="$(date +%s)"
dateForDirectory="$(date +"%Y-%m-%d_%H-%M-%S" -d "@${now}")"
dateForBundle="$(date +"%Y-%m-%dT%H:%M:%S%:z" -d "@${now}")"

measureName="$(jq -c --raw-output '.name' /app/measure.json)"
outputDir="$dateForDirectory-$measureName"
if [ "$SEND_REPORT_TO_SERVER" != true ]; then
  mkdir -p /app/output/"$outputDir"
  cp /app/measure.json /app/output/"$outputDir"/measure.json
fi

TRUSTSTORE_FILE="/app/truststore/self-signed-truststore.jks"
TRUSTSTORE_PASS=${TRUSTSTORE_PASS:-changeit}
KEY_PASS=${KEY_PASS:-changeit}

shopt -s nullglob
IFS=$'\n'
ca_files=(certs/*.pem)

if [ ! "${#ca_files[@]}" -eq 0 ]; then

    echo "# At least one CA file with extension *.pem found in certs folder -> starting fhir data evaluator with own CAs"

    if [[ -f "$TRUSTSTORE_FILE" ]]; then
          echo "## Truststore already exists -> resetting truststore"
          rm "$TRUSTSTORE_FILE"
    fi

    keytool -genkey -alias self-signed-truststore -keyalg RSA -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -keypass "$KEY_PASS" -dname "CN=self-signed,OU=self-signed,O=self-signed,L=self-signed,S=self-signed,C=TE"
    keytool -delete -alias self-signed-truststore -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -noprompt

    for filename in "${ca_files[@]}"; do

      echo "### ADDING CERT: $filename"
      keytool -delete -alias "$filename" -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -noprompt > /dev/null 2>&1
      keytool -importcert -alias "$filename" -file "$filename" -keystore "$TRUSTSTORE_FILE" -storepass "$TRUSTSTORE_PASS" -noprompt

    done

    java -Djavax.net.ssl.trustStore="$TRUSTSTORE_FILE" -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASS" -jar fhir-data-evaluator.jar "$outputDir" "$dateForBundle"
else
    echo "# No CA *.pem cert files found in /app/certs -> starting fhir data evaluator without own CAs"
    java -jar fhir-data-evaluator.jar "$outputDir" "$dateForBundle"
fi


if [ "${CONVERT_TO_CSV}" = true ]; then
  bash /app/csv-converter.sh /app/output/"$outputDir"
fi
