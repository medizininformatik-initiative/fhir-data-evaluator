#!/bin/bash -e

if [ ! -w /app/output ]; then
    echo "Missing writing permissions on output directory" >&2
    exit 1
fi

today=$(date +"%Y-%m-%d_%H-%M-%S")
measureName="$(jq -c --raw-output '.name' /app/measure.json)"
outputDir="$today-$measureName"
mkdir -p /app/output/"$outputDir"
cp /app/measure.json /app/output/"$outputDir"/measure.json

java -jar fhir-data-evaluator.jar "$outputDir"

if [ "${CONVERT_TO_CSV}" = true ]; then
  bash /app/csv-converter.sh /app/output/"$outputDir"
fi
