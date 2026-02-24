## Run

The Fhir Data Evaluator takes a FHIR Measure as input and creates a MeasureReport as output. 
An example of a Measure can be found [here](https://github.com/medizininformatik-initiative/fhir-data-evaluator/blob/v1.3.2/docs/example-measures/example-measure-1.json).
The resulting MeasureReport can either be viewed as file, or automatically be uploaded to a FHIR server 
(see: [Configuration](configuration.md)).

Minimal example to run the Fhir Data Evaluator:
```sh
docker run -v <your/measurefile.json>:/app/measure.json -e FHIR_SOURCE_SERVER=<http://your-fhir-server/fhir> -it ghcr.io/medizininformatik-initiative/fhir-data-evaluator:1.3.2
```

### Using the CSV-Converter:
The [CSV-Converter](https://github.com/medizininformatik-initiative/fhir-data-evaluator/blob/v1.3.2/csv-converter.sh) is a 
bash script that creates csv-files that represent the MeasureReport in a more human-readable form. One csv-file is 
generated for each stratifier in the MeasureReport. As input, it takes a MeasureReport file or a URL that points to a
MeasureReport resource which it will download, and an output directory where the csv-files should be written to. If the 
MeasureReport is configured as a URL, it can optionally use basic authentication or oauth for downloading the MeasureReport
from the FHIR sever.

Usage:
```
./csv-converter.sh <measure-report> <output-dir>
./csv-converter.sh <fhir-url> <output-dir> [-u <user> -p <password>]
./csv-converter.sh <fhir-url> <output-dir> [-i <issuer-url> -c <client-id> -s <client-secret>]
```
