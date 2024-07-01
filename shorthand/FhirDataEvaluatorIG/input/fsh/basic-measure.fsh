Profile: FhirDataEvaluatorBasicMeasure
Parent: http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/measure-cqfm
Description: "The Profile for the Measures used in the Fhir Data Evaluator"

* group 1..
* group.stratifier.criteria.language = #text/fhirpath
* group.stratifier.component.criteria.language = #text/fhirpath

* group.stratifier obeys criteriaRequiresCode
* group.stratifier obeys codeRequiresCriteria
* group.stratifier.component obeys criteriaRequiresCode

* group.population ^slicing.discriminator.type = #value
* group.population ^slicing.discriminator.path = "code.coding.code"
* group.population ^slicing.rules = #open
* group.population ^slicing.ordered = false

* group.population 1..1
* group.population contains initialPopulation 1..1
* group.population[initialPopulation].code.coding.system = "http://terminology.hl7.org/CodeSystem/measure-population"
* group.population[initialPopulation].code.coding.code = #initial-population
* group.population[initialPopulation].criteria.language = #text/x-fhir-query
