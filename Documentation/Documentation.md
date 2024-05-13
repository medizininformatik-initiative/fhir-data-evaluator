# Fhir Data Evaluator

The Fhir Data Evaluator takes a FHIR Measure resource as an input and returns a corresponding FHIR MeasureReport by 
executing FHIR Search queries and evaluating the resulting FHIR resources.

## Measure

### Group
The Measure uses groups to distinguish between different populations. 

In the FHIR specification, a population consists 
of *at least* one population criteria, but the Fhir Data Evaluator currently only supports the population criteria of type 
`initial-population` (Read more: https://build.fhir.org/valueset-measure-population.html). So currently groups are mainly 
used to define the base resources that should be evaluated.


### Stratifier
A Stratifier further evaluates the resources of the group populations.

The stratifier field is a list of stratifier elements, that each can consist of ether criteria or components, but not
both at the same time. The code of a stratifier element can be a custom but unique coding that roughly describes the 
stratifier. If the stratifier element consists of components, it still must have a code. Each component also
consists of a code and a criteria. A criteria consists of a language and an expression. As language, currently only
`text/fhirpath` is accepted. Accordingly, the expression must be a FHIRPath statement. It must start at the base resource
type and must evaluate into a coding, like `Condition.code.coding`. Multiple stratifier elements are evaluated separately
and only share the same base group population.

As currently only the population of type `initial-population` is supported, a stratifier element simply counts the
occurrences of each coding found at the path defined in the criteria expression, or in case the stratifier consists of 
components, each unique found *set* of codings.

* Example with a [single criteria](example-measures/example-measure-1.json)
* Example with [components](example-measures/example-measure-3.json)


## MeasureReport

Each group of the Measure results in a corresponding group in the MeasureReport. Also, each stratifier element in the
Measure results in a corresponding stratifier element in the MeasureReport. Each found coding of a stratifier element,
or in case the stratifier consists of components, each unique found *set* of codings results in a stratum element.
The population of the group indicates the overall count of the found resources. The population of a stratum element 
indicates the count of the found coding/ set of codings.

* Example [MeasureReport](example-measure-reports/example-measure-report-1.json)
