#!/bin/bash -e

if [ ! -w /app/output ]; then
    echo "Missing writing permissions on output directory" >&2
    exit 1
fi

vars_for_upload=(FHIR_REPORT_DESTINATION_SERVER AUTHOR_IDENTIFIER_SYSTEM AUTHOR_IDENTIFIER_VALUE PROJECT_IDENTIFIER_SYSTEM PROJECT_IDENTIFIER_VALUE)
all_vars_for_upload_set=true
if [ "${SEND_REPORT_TO_SERVER}" = true ]; then
   for var in "${vars_for_upload[@]}"; do
       if [[ -z "${!var}" ]]; then
           all_vars_for_upload_set=false
           break
       fi
   done
   if [ "$all_vars_for_upload_set" = false ]; then
      echo "In order to upload the MeasureReport to a FHIR server, all following environment variables must be set: ${vars_for_upload[*]}"
      exit 1
   fi
fi

today=$(date +"%Y-%m-%d_%H-%M-%S")
measureName="$(jq -c --raw-output '.name' /app/measure.json)"
outputDir="$today-$measureName"
if [ "$SEND_REPORT_TO_SERVER" != true ]; then
  mkdir -p /app/output/"$outputDir"
  cp /app/measure.json /app/output/"$outputDir"/measure.json
fi


UPLOAD_BUNDLE=""
if [ "$SEND_REPORT_TO_SERVER" = true ] && [ "$all_vars_for_upload_set" = true ]; then
    DOC_REF_ID="$(uuidgen)"
    REPORT_ID="$(uuidgen)"
    DATE="$(date +"%Y-%m-%dT%H:%M:%S%:z")"
    UPLOAD_BUNDLE=$(jq --arg DOC_REF_ID "$DOC_REF_ID" --arg REPORT_ID "$REPORT_ID" --arg DATE "$DATE" \
                '.entry[0].resource.author[0].identifier.system = env.AUTHOR_IDENTIFIER_SYSTEM |
                 .entry[0].resource.author[0].identifier.value = env.AUTHOR_IDENTIFIER_VALUE |
                 .entry[0].resource.masterIdentifier.system = env.PROJECT_IDENTIFIER_SYSTEM |
                 .entry[0].resource.masterIdentifier.value = env.PROJECT_IDENTIFIER_VALUE |
                 .entry[0].resource.content[0].attachment.url = "urn:uuid:" + $REPORT_ID |
                 .entry[0].resource.date = $DATE |
                 .entry[0].fullUrl = "urn:uuid:" + $DOC_REF_ID |
                 .entry[1].fullUrl = "urn:uuid:" + $REPORT_ID' \
            "/app/transaction-bundle-template.json")

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

    java -Djavax.net.ssl.trustStore="$TRUSTSTORE_FILE" -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASS" -jar fhir-data-evaluator.jar "$outputDir" "$UPLOAD_BUNDLE"
else
    echo "# No CA *.pem cert files found in /app/certs -> starting fhir data evaluator without own CAs"
    java -jar fhir-data-evaluator.jar "$outputDir" "$UPLOAD_BUNDLE"
fi


if [ "${CONVERT_TO_CSV}" = true ]; then
  bash /app/csv-converter.sh /app/output/"$outputDir"
fi


