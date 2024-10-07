#!/bin/bash -e

TRUSTSTORE_FILE="/app/truststore/self-signed-truststore.jks"
TRUSTSTORE_PASS=${TRUSTSTORE_PASS:-changeit}
KEY_PASS=${KEY_PASS:-changeit}

if [ ! -w /app/output ]; then
    echo "Missing writing permissions on output directory" >&2
    exit 1
fi

today=$(date +"%Y-%m-%d_%H-%M-%S")
measureName="$(jq -c --raw-output '.name' /app/measure.json)"
outputDir="$today-$measureName"
mkdir -p /app/output/"$outputDir"
cp /app/measure.json /app/output/"$outputDir"/measure.json

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

    java -Djavax.net.ssl.trustStore="$TRUSTSTORE_FILE" -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASS" -jar fhir-data-evaluator.jar "$outputDir"
else
    echo "# No CA *.pem cert files found in /app/certs -> starting fhir data evaluator without own CAs"
    java -jar fhir-data-evaluator.jar "$outputDir"
fi


if [ "${CONVERT_TO_CSV}" = true ]; then
  bash /app/csv-converter.sh /app/output/"$outputDir"
fi
