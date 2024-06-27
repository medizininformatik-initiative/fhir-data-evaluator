
Instance: ExampleConditionIcd10AndPatCount
InstanceOf: FhirDataEvaluatorCVMeasure
Description: "Example Measure to count all ICD-10 codes and the patient count."
* status = #active
* url = "https://medizininformatik-initiative.de/fhir/fdpg/Measure/ExampleConditionIcd10AndPatCount"
* version = "1.0"
* name = "ExampleConditionIcd10AndPatCount"
* experimental = false
* publisher = "FDPG-Plus"
* description = "Example Measure to count all ICD-10 codes and the patient count."

* group[0].id = "group-1"
* group[0].population[initialPopulation].code.coding.code = #initial-population
* group[0].population[initialPopulation].code.coding.system = "http://terminology.hl7.org/CodeSystem/measure-population"
* group[0].population[initialPopulation].criteria.expression = "Condition?_profile=https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose"
* group[0].population[initialPopulation].criteria.language = #text/x-fhir-query
* group[0].population[initialPopulation].id = "initial-population-identifier"

* group[0].population[measurePopulation].code.coding.code = #measure-population
* group[0].population[measurePopulation].code.coding.system = "http://terminology.hl7.org/CodeSystem/measure-population"
* group[0].population[measurePopulation].criteria.expression = "Condition"
* group[0].population[measurePopulation].criteria.language = #text/fhirpath
* group[0].population[measurePopulation].id = "measure-population-identifier"

* group[0].population[measureObservation].code.coding.code = #measure-observation
* group[0].population[measureObservation].code.coding.system = "http://terminology.hl7.org/CodeSystem/measure-population"
* group[0].population[measureObservation].criteria.expression = "Condition.subject.reference"
* group[0].population[measureObservation].criteria.language = #text/fhirpath
* group[0].population[measureObservation].extension[aggregateMethod].valueCode = #unique-count
* group[0].population[measureObservation].extension[aggregateMethod].url = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-aggregateMethod"
* group[0].population[measureObservation].extension[criteriaReference].valueString = "measure-population-identifier"
* group[0].population[measureObservation].extension[criteriaReference].url = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference"
* group[0].population[measureObservation].id = "measure-observation-identifier"

* group[0].stratifier.criteria.language = #text/fhirpath
* group[0].stratifier.criteria.expression = "Condition.code.coding.where(system='http://fhir.de/CodeSystem/bfarm/icd-10-gm')"
* group[0].stratifier.code = http://fhir-data-evaluator/strat/system#"icd10-code"
* group[0].stratifier.id = "strat-1"

Instance: ExampleConditionSnomedWithStatus
InstanceOf: FhirDataEvaluatorBasicMeasure
Description: "Example Measure to count all Snomed codes with clinical status."
* status = #active
* url = "https://medizininformatik-initiative.de/fhir/fdpg/Measure/ExampleConditionIcd10AndPatCount"
* version = "1.0"
* name = "ExampleConditionIcd10AndPatCount"
* experimental = false
* publisher = "FDPG-Plus"
* description = "Example Measure to count all ICD-10 codes and the patient count."

* group[0].id = "group-1"
* group[0].population[initialPopulation].code.coding.code = #initial-population
* group[0].population[initialPopulation].code.coding.system = "http://terminology.hl7.org/CodeSystem/measure-population"
* group[0].population[initialPopulation].criteria.expression = "Condition?_profile=https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose"
* group[0].population[initialPopulation].criteria.language = #text/x-fhir-query
* group[0].population[initialPopulation].id = "initial-population-identifier"

* group[0].stratifier.component[0].criteria.language = #text/fhirpath
* group[0].stratifier.component[0].criteria.expression = "Condition.code.coding.where(system='http://snomed.info/sct')"
* group[0].stratifier.component[0].code = http://fhir-data-evaluator/strat/system#"snomed-code"
* group[0].stratifier.component[1].criteria.language = #text/fhirpath
* group[0].stratifier.component[1].criteria.expression = "Condition.clinicalStatus.coding"
* group[0].stratifier.component[1].code = http://fhir-data-evaluator/strat/system#"condition-clinical-status"
* group[0].stratifier.id = "strat-1"
