# Fhir Data Evaluator

## Overview

The aim of the project is to provide a tool, which can be used to extract metadata information from multiple FHIR servers and combine the data to:
1. Get an overview of how many patients are available for each criterion as identified by the coding of a resources main identifier (e.g. Condition.code.coding, Observation.code, Specimen.type)
2. Get a better understanding of the available data and the actual values in a FHIR server
3. Identify missing and incorrect values

The FHIR Data Evaluator is a command line program, which based on a FHIR Input Measure configuration iterates through FHIR resources on a FHIR server
and calculates stratifier or statistical counts for values of fields of the evaluated resources. The output is a FHIR MeasureReport.
The MeasureReport can be converted to a more human-readable CSV format using the [CSV-Converter](#using-the-csv-converter).
For example configuring the evaluator to stratify the icd10 code field of the condition resource (FHIR path: 
Condition.code.coding.where(system='http://fhir.de/CodeSystem/bfarm/icd-10-gm')) would lead to the following output 
(once converted to csv):

```csv
"system","code","display","count","unique count"
"http://fhir.de/CodeSystem/bfarm/icd-10-gm","I95.0",,10811,10797
"http://fhir.de/CodeSystem/bfarm/icd-10-gm","I60.1",,4,4
"http://fhir.de/CodeSystem/bfarm/icd-10-gm","I22.8",,4,4
```
The Fhir Data Evaluator further counts the statistical counts per patient (as in: how many patients have this resource 
with this specific value - unique count above).

The following types of fields/expressions are currently supported:

* [Coding](https://www.hl7.org/fhir/datatypes.html#Coding), example: `Condition.code.coding`
* [boolean](https://www.hl7.org/fhir/datatypes.html#boolean), example: `Condition.code.exists()`
* [code](https://www.hl7.org/fhir/datatypes.html#code), example: `Patient.gender`

For a more detailed Documentation see: * [Documentation](Documentation/Documentation.md)

## Run

When running the Fhir Data Evaluator with Docker, it will require a Measure resource file as input. The resulting
MeasureReport will be saved inside `/app/output/` of the Docker container into a new Directory named after the current 
date combined with the Measure's name (specified in the 'name' field of the Measure).

An example of a Measure can be found [here](Documentation/example-measures/example-measure-1.json).

### MeasureReport as Output only:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SOURCE_SERVER=<http://your-fhir-server/fhir> -e TZ=Europe/Berlin -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:1.2.0
```
* this generates a MeasureReport that is stored in a directory named after the current date combined with the Measure's 
name inside `/app/output`. In order to see the MeasureReport, one can for example mount a volume to `/app/output/` like
above.

### Using the CSV-Converter:
The [CSV-Converter](csv-converter.sh) is a bash script that creates csv-files that represent the MeasureReport. One csv
file is generated for each stratifier in the MeasureReport.
* As input, it takes a MeasureReport file, or a URL that points to a MeasureReport resource, and an output directory where
the csv files should be written to
* if the input is a URL, it can optionally use basic authentication or oauth:
```
Usage: 
./csv-converter.sh <measure-report> <output-dir>
./csv-converter.sh <fhir-url> <output-dir> [-u <user> -p <password>]
./csv-converter.sh <fhir-url> <output-dir> [-i <issuer-url> -c <client-id> -s <client-secret>]
```

### Usage with Docker Networks
* to any of the listed docker run commands add ```--network <your_network>``` to run the container inside a Docker network
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SOURCE_SERVER=<http://your-fhir-server/fhir> -e TZ=Europe/Berlin --network <your_network> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:1.2.0
```

### Resolving References
In case the FHIR Path of a stratifier evaluates a reference, the referenced resources must be included in the initial population
as 'include' resources.

Example:
* FHIR Path of the stratifier: `MedicationAdministration.medication.resolve().ofType(Medication).code.coding`
* FHIR Search query of the initial population: `MedicationAdministration?_include=MedicationAdministration:medication`

If a reference cannot be resolved, the stratifier is counted as a value that was not found.

### Time Zones
When generating the MeasureReport, the output files will be saved in a directory named after the current date
combined with the Measure's name. Since it is run inside a Docker container, the time zone might differ from the one on
the host machine. This time zone is also used to set the date for the DocumentReference if the MeasureReport is sent to
a FHIR server. If you want to match the time zones, set the time zone for example to ```-e TZ=Europe/Berlin```:

### Passing Additional Environment Variables:

The environment variables are used inside the docker container, so if they are set only on the host machine, they won't
be visible in the container. Each additional environment variable can be passed using the `-e` flag.
* Example of passing a page count of 50:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SOURCE_SERVER=<http://your-fhir-server/fhir> -e FHIR_SOURCE_PAGE_COUNT=50 -e TZ=Europe/Berlin -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:1.2.0
```

### Sending the MeasureReport to a FHIR Server

If `SEND_REPORT_TO_SERVER` is set to true, the MeasureReport is sent to the `FHIR_REPORT_SERVER` along with a
DocumentReference that is configured with the following environment variables:
* `AUTHOR_IDENTIFIER_SYSTEM` (example: `http://dsf.dev/sid/organization-identifier`)
* `AUTHOR_IDENTIFIER_VALUE` (example: `Test_DIC1`)
* `PROJECT_IDENTIFIER_SYSTEM` (example: `http://medizininformatik-initiative.de/sid/project-identifier`)
* `PROJECT_IDENTIFIER_VALUE` (example: `Test_PROJECT_Evaluation`)

## Environment Variables

| Name                            | Default                                                       | Description                                                                                           |
|:--------------------------------|:--------------------------------------------------------------|:------------------------------------------------------------------------------------------------------|
| FHIR_SOURCE_SERVER              | http://localhost:8080/fhir                                    | The base URL of the FHIR server to use for downloading the resources.                                 |
| FHIR_SOURCE_USER                |                                                               | The username to use for HTTP Basic Authentication for the source FHIR server.                         |
| FHIR_SOURCE_PASSWORD            |                                                               | The password to use for HTTP Basic Authentication for the source FHIR server.                         |
| FHIR_SOURCE_MAX_CONNECTIONS     | 4                                                             | The maximum number of connections to open towards the source FHIR server.                             |
| FHIR_SOURCE_PAGE_COUNT          | 1000                                                          | The number of resources per page to request from the source FHIR server.                              |
| FHIR_SOURCE_BEARER_TOKEN        |                                                               | Bearer token for authentication for the source FHIR server.                                           |
| FHIR_SOURCE_OAUTH_ISSUER_URI    |                                                               | The issuer URI of the OpenID Connect provider for the source FHIR server.                             |
| FHIR_SOURCE_OAUTH_CLIENT_ID     |                                                               | The client ID to use for authentication with OpenID Connect provider for the source FHIR server.      |
| FHIR_SOURCE_OAUTH_CLIENT_SECRET |                                                               | The client secret to use for authentication with OpenID Connect provider for the source FHIR server.  |
| FHIR_REPORT_SERVER              | http://localhost:8080/fhir                                    | The base URL of the FHIR server to use for (optionally) uploading the MeasureReport.                  |
| FHIR_REPORT_USER                |                                                               | The username to use for HTTP Basic Authentication for the Report FHIR server.                         |
| FHIR_REPORT_PASSWORD            |                                                               | The password to use for HTTP Basic Authentication for the Report FHIR server.                         |
| FHIR_REPORT_MAX_CONNECTIONS     | 4                                                             | The maximum number of connections to open towards the Report FHIR server.                             |
| FHIR_REPORT_BEARER_TOKEN        |                                                               | Bearer token for authentication for the Report FHIR server.                                           |
| FHIR_REPORT_OAUTH_ISSUER_URI    |                                                               | The issuer URI of the OpenID Connect provider for the Report FHIR server.                             |
| FHIR_REPORT_OAUTH_CLIENT_ID     |                                                               | The client ID to use for authentication with OpenID Connect provider for the Report FHIR server.      |
| FHIR_REPORT_OAUTH_CLIENT_SECRET |                                                               | The client secret to use for authentication with OpenID Connect provider for the Report FHIR server.  |
| MAX_IN_MEMORY_SIZE_MIB          | 10                                                            | The maximum in-memory buffer size for each webclient in MiB.                                          |
| TZ                              | Europe/Berlin                                                 | The time zone used to create the output directory and set the date in the DocumentReference.          |
| SEND_REPORT_TO_SERVER           | false                                                         | Whether the MeasureReport should be sent to the FHIR Report server.                                   |
| AUTHOR_IDENTIFIER_SYSTEM        | http://dsf.dev/sid/organization-identifier                    | The system of the author organization used when uploading the report.                                 |
| AUTHOR_IDENTIFIER_VALUE         |                                                               | The code of the author organization used when uploading the report.                                   |
| PROJECT_IDENTIFIER_SYSTEM       | http://medizininformatik-initiative.de/sid/project-identifier | The system of the master identifier used when uploading the report.                                   |
| PROJECT_IDENTIFIER_VALUE        |                                                               | The value of the master identifier used when uploading the report.                                    |


## Documentation

* [Documentation](Documentation/Documentation.md)
