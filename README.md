
# Fhir Data Evaluator


## Goal

The goal of this project is to provide a service that allows for evaluation of FHIR Measure resources.

## Run

When running the Fhir Data Evaluator with Docker, it will require a Measure resource file as input and save the resulting
MeasureReport into a Directory named after the current date combined with the Measure's name (specified in the 'name' field).
If specified, additional CSV files will be created that represent the MeasureReport.

An example of a Measure can be found [here](Documentation/example-measures/example-measure-1.json).

### MeasureReport as Output only:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SERVER=<http://your-fhir-server/fhir> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:latest
```
### MeasureReport and CSV Output:

```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e CONVERT_TO_CSV=true -e FHIR_SERVER=<http://your-fhir-server/fhir> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:latest
```
* this generates a CSV file for each Stratifier and stores the files in a directory named after the current date combined 
with the Measure's name
* if this is run multiple times on the same day, it will override the files

### Usage with Docker Networks
* to any of the listed docker run commands add ```--network <your_network>``` to run the container inside a Docker network
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e CONVERT_TO_CSV=true -e FHIR_SERVER=<http://your-fhir-server/fhir> --network <your_network> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:latest
```

### Passing Additional Environment Variables:

The environment variables are used inside the docker container, so if they are set only on the host machine, they won't
be visible in the container. Each additional environment variable can be passed using the `-e` flag.
* Example of passing a page count of 50:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SERVER=<http://your-fhir-server/fhir> -e FHIR_PAGE_COUNT=50 -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:latest
```

### Time Zones
When generating the CSV files from the MeasureReport, the files will be saved in a directory named after the current date 
combined with the Measure's name. Since it is run inside a Docker container, the time zone might differ from the one on 
the host machine. If you want to match the time zones, add for example ```-e TZ=Europe/Berlin```:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e CONVERT_TO_CSV=true -e FHIR_SERVER=<http://your-fhir-server/fhir> -e TZ=Europe/Berlin -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:latest
```

## Environment Variables

| Name                   | Default                    | Description                                                                 |
|:-----------------------|:---------------------------|:----------------------------------------------------------------------------|
| FHIR_SERVER            | http://localhost:8080/fhir | The base URL of the FHIR server to use.                                     |
| FHIR_USER              |                            | The username to use for HTTP Basic Authentication.                          |
| FHIR_PASSWORD          |                            | The password to use for HTTP Basic Authentication.                          |
| FHIR_MAX_CONNECTIONS   | 4                          | The maximum number of connections to open towards the FHIR server.          |
| FHIR_MAX_QUEUE_SIZE    | 500                        | The maximum number FHIR server requests to queue before returning an error. |
| FHIR_PAGE_COUNT        | 1000                       | The number of resources per page to request from the FHIR server.           |
| FHIR_BEARER_TOKEN      |                            | Bearer token for authentication.                                            |
| MAX_IN_MEMORY_SIZE_MIB | 10                         | The maximum in-memory buffer size for the webclient in MiB.                 |
| GENERATE_CSV           | false                      | Whether for the MeasureReport should be generated CSV files.                |


## Documentation

* [Documentation](Documentation/Documentation.md)
