#!/bin/bash -e

MEASURE_FILE="$1"
OUTPUT_DIR="$2/performance-test-output"
ITERATIONS="$3"
DOCKER_IMAGE="$4"

# clear output directory
rm -rf "${OUTPUT_DIR:?}"/*/
mkdir -p "$OUTPUT_DIR"

for ((i=0;i<ITERATIONS;i++))
do
  docker run -v "/$MEASURE_FILE":/app/measure.json -v "/$OUTPUT_DIR":/app/output/ -e FHIR_SOURCE_SERVER=http://host.docker.internal:8080/fhir -e FHIR_SOURCE_PAGE_COUNT=500 -it "$DOCKER_IMAGE"
done

result=$(\
for d in "${OUTPUT_DIR:?}"/*/ ; do
    jq ".extension[0].valueQuantity.value" "$d/measure-report.json"
done | awk '{sum += $1; sumsq += $1^2} END {printf("{\"avg\": %f, \"stddev\": %f}", sum/NR, sqrt(sumsq/NR - (sum/NR)^2))}' |
      jq --raw-output '["average", "standard deviation"],
          [.avg, .stddev] | @csv')

echo "$result"
