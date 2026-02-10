# Fhir Data Evaluator

The Fhir Data Evaluator takes a FHIR Measure resource as an input and returns a corresponding FHIR MeasureReport by 
executing FHIR Search queries and evaluating the resulting FHIR resources with FHIRPath.

## Measure

### Group
The Measure uses groups to distinguish between different populations. 

Every population must have one population criteria of type `initial-population`. The initial population defines the 'base',
on which the evaluator operates, with a FHIR Search Query. So if the evaluator should operate on Condition resources, the 
FHIR Search Query expression for the initial population would be "`Condition`". There can also be other populations in 
addition to the initial population. Read more about how to use the `measure-population`and `measure-observation` 
[here](#other-populations) to for example count not all encountered resources but only unique patient ID's.


### Stratifier
A Stratifier groups the resources of a population by the value returned by its expression(s).

The stratifier field is a list of stratifier elements, that each can consist of either criteria or components, but not
both at the same time. The code of a stratifier element can be a custom but unique coding that roughly describes the 
stratifier. Each component also
consists of a code and a criteria. A criteria consists of a language and an expression. As language, currently only
`text/fhirpath` is accepted. Accordingly, the expression must be a FHIRPath statement. It must start at the base resource
type and must evaluate into one of the following types: 
* [Coding](https://www.hl7.org/fhir/datatypes.html#Coding), example: `Condition.code.coding` 
* [boolean](https://www.hl7.org/fhir/datatypes.html#boolean), example: `Condition.code.exists()` 
* [code](https://www.hl7.org/fhir/datatypes.html#code), example: `Patient.gender` 

Multiple stratifier elements are evaluated separately and only share the same base group population.

Each found value (= stratum) has its own populations. The `initial-population` in a stratum represents
the count of the value found at the path defined in the criteria expression, or in case the stratifier 
consists of components, it represents the count of each unique found *set* of values.
All [other populations](#other-populations) are also evaluated for each stratum if they are present.

* Example with a [single criteria](example-measures/example-measure-1.json)
* Example with [components](example-measures/example-measure-3.json)


### Other Populations

* Measure Population:
  * used to further narrow down the base population using FHIRPath
  * counts this reduced population
  * acts as base for the measure observation, if the measure observation is present

* Measure Observation:
  * currently must evaluate into type `String` with FHIRPath
  * found values are also counted, but mainly used to aggregate a score
  * must have an aggregate method extension with value type `unique-count`, which tells the Fhir Data Evaluator to 
  aggregate a unique count of the values
  * must have a criteria reference extension that references the measure population
  * the result of the aggregate method is separately calculated on both group and on stratum level and stored in the 
  `measureScore`

* in case these populations are used, the measure will be a continuous-variable measure, which requires 
[this](http://fhir-data-evaluator/StructureDefinition/FhirDataEvaluatorContinuousVariableMeasure) profile
* other populations that are defined in FHIR, such as the `numerator`, are currently not supported by the Fhir Data Evaluator


## MeasureReport

Each group of the Measure results in a corresponding group in the MeasureReport. Also, each stratifier element in the
Measure results in a corresponding stratifier element in the MeasureReport. Each found value of a stratifier element,
or in case the stratifier consists of components, each unique found *set* of values results in a stratum element.
The initial population of the group represents the overall count of the found resources. The initial population of a 
stratum element represents the count of the found values/ set of values.

* Example [MeasureReport](example-measure-reports/example-measure-report-1.json)


## Profiles and Validation

There are two profiles that are supported by the Fhir Data Evaluator:
* Basic Measure:
  * Profile: http://fhir-data-evaluator/StructureDefinition/FhirDataEvaluatorBasicMeasure
  * is used when there is only an initial population without any other population
* Continuous Variable Measure:
  * Profile: http://fhir-data-evaluator/StructureDefinition/FhirDataEvaluatorContinuousVariableMeasure
  * is used when there is a need for a measure population and measure observation population 
* in either case, the Fhir Data Evaluator adheres to the
  [HL7 Quality Measure Implementation Guide ](https://hl7.org/fhir/us/cqfmeasures/measure-conformance.html)

The resources to validate a measure can be built with [SUSHI](https://github.com/FHIR/sushi). To do this, you must cd 
into `shorthand/FhirDataEvaluatorIG` and run `sushi build`. The resulting resources will be saved at 
`shorthand/FhirDataEvaluatorIG/fhs-generated/resources` and can be used for example with the 
[FHIR Validator](https://confluence.hl7.org/display/FHIR/Using+the+FHIR+Validator). This also generates the measures that
are used in the integration test, and they are copied into the test resources directory
at `src/test/resources/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorTest/Measures`
during the maven build process.
