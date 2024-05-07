MEASURE_REPORT=$(java -jar fhir-data-evaluator.jar --measure-file=/app/input-measure.json --fhir-server=${FHIR_SERVER})

if [ "${CONVERT_TO_CSV}" = true ]; then
  echo $MEASURE_REPORT | /app/csv-converter.sh /app/csv-output/
else
  echo $MEASURE_REPORT
fi
