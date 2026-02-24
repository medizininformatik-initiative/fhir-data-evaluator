# Fhir Data Evaluator

[![GitHub[+] Release](https://img.shields.io/github/v/release/medizininformatik-initiative/fhir-data-evaluator?sort=date&display_name=tag&style=flat&logo=github&label=current)]() [![Docs](https://img.shields.io/badge/Docs-green.svg)](https://medizininformatik-initiative.github.io/fhir-data-evaluator/)

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


## Documentation
For more detailed instructions on usage and configuration, see the [Documentation](https://medizininformatik-initiative.github.io/fhir-data-evaluator/).



