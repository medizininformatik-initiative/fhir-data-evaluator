# Configuration

### MeasureReport as File only:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SOURCE_SERVER=<http://your-fhir-server/fhir> -e TZ=Europe/Berlin -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:1.3.3
```
This generates a MeasureReport that is stored in a directory named after the current date combined with the Measure's
name inside `/app/output`. In order to see the MeasureReport, one can for example mount a volume to `/app/output/` like
above.

### Sending the MeasureReport to a FHIR Server

If `SEND_REPORT_TO_SERVER` is set to `true`, the MeasureReport is sent to the `FHIR_REPORT_SERVER` along with a
DocumentReference that is configured with the following environment variables:
* `AUTHOR_IDENTIFIER_SYSTEM` (example: `http://dsf.dev/sid/organization-identifier`)
* `AUTHOR_IDENTIFIER_VALUE` (example: `Test_DIC1`)
* `PROJECT_IDENTIFIER_SYSTEM` (example: `http://medizininformatik-initiative.de/sid/project-identifier`)
* `PROJECT_IDENTIFIER_VALUE` (example: `Test_PROJECT_Evaluation`)


### Usage with Docker Networks
To any of the listed "docker run" commands add ```--network <your_network>``` to run the container inside a Docker network:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SOURCE_SERVER=<http://your-fhir-server/fhir> -e TZ=Europe/Berlin --network <your_network> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:1.3.3
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

The Fhir Data Evaluator is configured via environment variables that must be passed to the docker container. 
If the environment variables are set only on the host machine, they won't be visible in the container. 
Any environment variable can be passed to the container using the `-e` flag.
* Example of passing a page count of 50:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -v <your/output/dir>:/app/output -e FHIR_SOURCE_SERVER=<http://your-fhir-server/fhir> -e FHIR_SOURCE_PAGE_COUNT=50 -e TZ=Europe/Berlin -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:1.3.3
```

### Obfuscation
By default, a MeasureReport is generated that contains the exact counts of elements found in the FHIRServer.
If the environment variable `CREATE_OBFUSCATED_REPORT` is set to `true`, an additional obfuscated MeasureReport is generated next to
the default MeasureReport, where all counts in the report are obfuscated by the following rule:
```
if exact_count >= 1 AND exact_count <= OBFUSCATION_COUNT:
    obfuscated_count = OBFUSCATION_COUNT
else:
    obfuscated_count = exact_count
```
i.e. if OBFUSCATION_COUNT is set to 5, only numbers from 1 to 4 are rounded to 5. Exact 0 and everything above 5 is not changed.

If `SEND_REPORT_TO_SERVER` is set to true, the raw report and the obfuscated report are both sent to the report server.
Therefore, the following variables must be additionally set:
* `PROJECT_IDENTIFIER_SYSTEM_OBFUSCATED_REPORT`
* `PROJECT_IDENTIFIER_VALUE_OBFUSCATED_REPORT`

For the author identifier of the obfuscated report, the same identifier is used as for the non-obfuscated report.

## Environment Variables
#### `FHIR_SOURCE_SERVER`

The base URL of the FHIR server to use for downloading the resources.

**Default:** - `http://localhost:8080/fhir`

---

#### `FHIR_SOURCE_USER`

The username to use for HTTP Basic Authentication for the source FHIR server.

**Default:** - ``

---

#### `FHIR_SOURCE_PASSWORD`

The password to use for HTTP Basic Authentication for the source FHIR server.

**Default:** - ``

---

#### `FHIR_SOURCE_MAX_CONNECTIONS`

The maximum number of connections to open towards the source FHIR server.

**Default:** - `4`

---

#### `FHIR_SOURCE_PAGE_COUNT`

The number of resources per page to request from the source FHIR server.

**Default:** - `1000`

---

#### `FHIR_SOURCE_BEARER_TOKEN`

Bearer token for authentication for the source FHIR server.

**Default:** - ``

---

#### `FHIR_SOURCE_OAUTH_ISSUER_URI`

The issuer URI of the OpenID Connect provider for the source FHIR server.

**Default:** - ``

---

#### `FHIR_SOURCE_OAUTH_CLIENT_ID`

The client ID to use for authentication with OpenID Connect provider for the source FHIR server.

**Default:** - ``

---

#### `FHIR_SOURCE_OAUTH_CLIENT_SECRET`

The client secret to use for authentication with OpenID Connect provider for the source FHIR server.

**Default:** - ``

---

#### `FHIR_REPORT_SERVER`

The base URL of the FHIR server to use for (optionally) uploading the MeasureReport.

**Default:** - `http://localhost:8080/fhir`

---

#### `FHIR_REPORT_USER`

The username to use for HTTP Basic Authentication for the Report FHIR server.

**Default:** - ``

---

#### `FHIR_REPORT_PASSWORD`

The password to use for HTTP Basic Authentication for the Report FHIR server.

**Default:** - ``

---

#### `FHIR_REPORT_MAX_CONNECTIONS`

The maximum number of connections to open towards the Report FHIR server.

**Default:** - `4`

---

#### `FHIR_REPORT_BEARER_TOKEN`

Bearer token for authentication for the Report FHIR server.

**Default:** - ``

---

#### `FHIR_REPORT_OAUTH_ISSUER_URI`

The issuer URI of the OpenID Connect provider for the Report FHIR server.

**Default:** - ``

---

#### `FHIR_REPORT_OAUTH_CLIENT_ID`

The client ID to use for authentication with OpenID Connect provider for the Report FHIR server.

**Default:** - ``

---

#### `FHIR_REPORT_OAUTH_CLIENT_SECRET`

The client secret to use for authentication with OpenID Connect provider for the Report FHIR server.

**Default:** - ``

---

#### `MAX_IN_MEMORY_SIZE_MIB`

The maximum in-memory buffer size for each webclient in MiB.

**Default:** - `10`

---

#### `TZ`

The time zone used to create the output directory and set the date in the DocumentReference.

**Default:** - `Europe/Berlin`

---

#### `SEND_REPORT_TO_SERVER`

Whether the MeasureReport should be sent to the FHIR Report server.

**Default:** - `false`

---

#### `CREATE_OBFUSCATED_REPORT`

Whether an obfuscated MeasureReport should be created in addition.

**Default:** - `false`

---

#### `OBFUSCATION_COUNT`

The number to wich values are clamped (obfuscated).

**Default:** - `5`

---

#### `AUTHOR_IDENTIFIER_SYSTEM`

The system of the author organization used when uploading the report.

**Default:** - `http://dsf.dev/sid/organization-identifier`

---

#### `AUTHOR_IDENTIFIER_VALUE`

The code of the author organization used when uploading the report.

**Default:** - ``

---

#### `PROJECT_IDENTIFIER_SYSTEM`

The system of the master identifier used when uploading the report.

**Default:** - `http://medizininformatik-initiative.de/sid/project-identifier`

---

#### `PROJECT_IDENTIFIER_VALUE`

The value of the master identifier used when uploading the report.

**Default:** - ``

---

#### `PROJECT_IDENTIFIER_SYSTEM_OBFUSCATED_REPORT`

The system of the master identifier used when uploading the obfuscated report.

**Default:** - `http://medizininformatik-initiative.de/sid/project-identifier`

---


