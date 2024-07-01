Invariant: criteriaRequiresCode
Description: "If stratifier or component has criteria, it must have a coding with system and code."
Severity: #error
Expression: "criteria.exists() and component.exists().not() implies code.coding.code.exists() and code.coding.system.exists()"

Invariant: codeRequiresCriteria
Description: "If stratifier or component has code, it must have criteria."
Severity: #error
Expression: "code.exists() and component.exists().not() implies criteria.exists()"

Invariant: measurePopulationIdEqualsCriteriaReference
Description: "The referenced population in the criteriaReference extension of the measure observation must be equal to the id of the measure population."
Severity: #error
Expression: "population.where(code.coding.code = 'measure-population').id = population.where(code.coding.code = 'measure-observation').extension.where(url = 'http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference').valueString"
