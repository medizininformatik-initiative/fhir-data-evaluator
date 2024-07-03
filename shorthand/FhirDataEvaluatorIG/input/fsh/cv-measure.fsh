Profile: FhirDataEvaluatorContinuousVariableMeasure
Parent: http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cv-measure-cqfm
Description: "The Profile for the Measures used in the Fhir Data Evaluator"

* group 1..
* group.stratifier.criteria.language = #text/fhirpath
* group.stratifier.component.criteria.language = #text/fhirpath

* group.stratifier obeys criteriaRequiresCode
* group.stratifier obeys codeRequiresCriteria
* group.stratifier.component obeys criteriaRequiresCode

* group obeys measurePopulationIdEqualsCriteriaReference

* group.population[initialPopulation].criteria.language = #text/x-fhir-query
* group.population[measurePopulation].criteria.language = #text/fhirpath
* group.population[measureObservation].criteria.language = #text/fhirpath
* group.population[measureObservation].extension[aggregateMethod].valueCode = #unique-count
