
# Fhir Data Evaluator


## Overview

The aim of the project is to provide a tool, which can be used to extract metadata information from multiple FHIR servers and combine the data to:
1. Get an overview of how many patients are available for each criterion as identified by the coding of a resources main identifier (e.g. Condition.code.coding, Observation.code, Specimen.type)
2. Get a better understanding of the available data and the actual values in a FHIR server
3. Identify missing and incorrect values

The FHIR Data Evaluator is a command line program, which based on a FHIR Input Measure configuration iterates through FHIR resources on a FHIR server
and calculates stratifier or statistical counts for values of fields of the evaluated resources. The output is a FHIR MeasureReport (and if configured multiple csv files).

For example configuring the evaluator to stratify the icd10 code field of the condition resource (FHIR path: Condition.code.coding.where(system='http://fhir.de/CodeSystem/bfarm/icd-10-gm'))
would lead to the following output (once converted to csv):

```csv
"system","code","display","count","unique count"
"http://fhir.de/CodeSystem/bfarm/icd-10-gm","I95.0",,10811,10797
"http://fhir.de/CodeSystem/bfarm/icd-10-gm","I60.1",,4,4
"http://fhir.de/CodeSystem/bfarm/icd-10-gm","I22.8",,4,4
```
It further counts the statistical counts per patient (as in: how many patients have this resource with this specific value - unique count above).

The following types of fields/expressions are currently supported:

* [Coding](https://www.hl7.org/fhir/datatypes.html#Coding), example: `Condition.code.coding`
* [boolean](https://www.hl7.org/fhir/datatypes.html#boolean), example: `Condition.code.exists()`
* [code](https://www.hl7.org/fhir/datatypes.html#code), example: `Patient.gender`

For a more detailed Documentation see: * [Documentation](Documentation/Documentation.md)

## Run

When running the Fhir Data Evaluator with Docker, it will require a Measure resource file as input and save the resulting
MeasureReport into a Directory named after the current date combined with the Measure's name (specified in the 'name' field).
If specified, additional CSV files will be created that represent the MeasureReport.

An example of a Measure can be found [here](Documentation/example-measures/example-measure-1.json).

### MeasureReport as Output only:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SERVER=<http://your-fhir-server/fhir> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:0.1.0
```
### MeasureReport and CSV Output:

```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e CONVERT_TO_CSV=true -e FHIR_SERVER=<http://your-fhir-server/fhir> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:0.1.0
```
* this generates a CSV file for each Stratifier and stores the files in a directory named after the current date combined 
with the Measure's name
* if this is run multiple times on the same day, it will override the files

### Usage with Docker Networks
* to any of the listed docker run commands add ```--network <your_network>``` to run the container inside a Docker network
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e CONVERT_TO_CSV=true -e FHIR_SERVER=<http://your-fhir-server/fhir> --network <your_network> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:0.1.0
```

### Time Zones
When generating the CSV files from the MeasureReport, the files will be saved in a directory named after the current date
combined with the Measure's name. Since it is run inside a Docker container, the time zone might differ from the one on
the host machine. If you want to match the time zones, add for example ```-e TZ=Europe/Berlin```:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e CONVERT_TO_CSV=true -e FHIR_SERVER=<http://your-fhir-server/fhir> -e TZ=Europe/Berlin -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:0.1.0
```

### Passing Additional Environment Variables:

The environment variables are used inside the docker container, so if they are set only on the host machine, they won't
be visible in the container. Each additional environment variable can be passed using the `-e` flag.
* Example of passing a page count of 50:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SERVER=<http://your-fhir-server/fhir> -e FHIR_PAGE_COUNT=50 -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:0.1.0
```

## Environment Variables

| Name                   | Default                    | Description                                                                            |
|:-----------------------|:---------------------------|:---------------------------------------------------------------------------------------|
| FHIR_SERVER            | http://localhost:8080/fhir | The base URL of the FHIR server to use.                                                |
| FHIR_USER              |                            | The username to use for HTTP Basic Authentication.                                     |
| FHIR_PASSWORD          |                            | The password to use for HTTP Basic Authentication.                                     |
| FHIR_MAX_CONNECTIONS   | 4                          | The maximum number of connections to open towards the FHIR server.                     |
| FHIR_MAX_QUEUE_SIZE    | 500                        | The maximum number FHIR server requests to queue before returning an error.            |
| FHIR_PAGE_COUNT        | 1000                       | The number of resources per page to request from the FHIR server.                      |
| FHIR_BEARER_TOKEN      |                            | Bearer token for authentication.                                                       |
| MAX_IN_MEMORY_SIZE_MIB | 10                         | The maximum in-memory buffer size for the webclient in MiB.                            |
| CONVERT_TO_CSV         | false                      | Whether for the MeasureReport should be generated CSV files.                           |


## Documentation

* [Documentation](Documentation/Documentation.md)
