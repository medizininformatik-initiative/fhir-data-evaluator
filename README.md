
# Fhir Data Evaluator


## Goal

The goal of this project is to provide a service that allows for evaluation of FHIR Measure resources.

## Run

### MeasureReport Output:
```sh
docker run -v <your-measurefile.json>:/app/input-measure.json -e FHIR_SERVER=<http://your-fhir-server/fhir> fhir-data-evaluator:latest
```
### CSV Output:

```sh
docker run -v <your-measurefile.json>:/app/input-measure.json -v <your-output-dir/>:/app/csv-output/ -e CONVERT_TO_CSV=true -e FHIR_SERVER=<http://your-fhir-server/fhir> fhir-data-evaluator:latest
```
* this generates a CSV file for each Stratifier and stores the files in a directory with the current date as name
* if this is run multiple times on the same day, it will override the files

### Passing Additional Environment Variables:

The environment variables are used inside the docker container, so if they are set only on the host machine, they won't
be visible in the container. Each additional environment variable can be passed using the `-e` flag.
* Example of passing a page count of 50:
```sh
docker run -v <your-measurefile.json>:/app/input-measure.json -e FHIR_SERVER=<http://your-fhir-server/fhir> -e FHIR_PAGE_COUNT=50 fhir-data-evaluator:latest
```

## Environment Variables

| Name                 | Default                    | Description                                                                 |
|:---------------------|:---------------------------|:----------------------------------------------------------------------------|
| MEASURE_FILE         | example-measure.json       | The base URL of the FHIR server to use.                                     |
| FHIR_SERVER          | http://localhost:8082/fhir | The base URL of the FHIR server to use.                                     |
| FHIR_USER            |                            | The username to use for HTTP Basic Authentication.                          |
| FHIR_PASSWORD        |                            | The password to use for HTTP Basic Authentication.                          |
| FHIR_MAX_CONNECTIONS | 4                          | The maximum number of connections to open towards the FHIR server.          |
| FHIR_MAX_QUEUE_SIZE  | 500                        | The maximum number FHIR server requests to queue before returning an error. |
| FHIR_PAGE_COUNT      | 1000                       | The number of resources per page to request from the FHIR server.           |
| CONVERT_TO_CSV       | false                      | Whether the output MeasureReport should be converted into CSV format.       |
| CSV_OUTPUT_DIR       |                            | The Directory where the converted CSV files will be saved.                  |


## Documentation

* [Documentation](Documentation/Documentation.md)

## Development

To run the Fhir Data Evaluator in an IDE, the Entrypoint is not the Spring Application itself, but the main method in the
EvaluationExecutor class in [FhirDataEvaluatorApplication.java](src/main/java/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorApplication.java).
