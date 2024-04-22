package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.Objects;

/**
 * Represents a found value of a resource.
 */
public record ComponentKeyPair(ComponentKey definitionCode, ComponentKey valueCode) {

    public static ComponentKeyPair ofFailedInvalidType(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, new HashableCoding("http://fhir-evaluator/strat/system", "fail-invalid-type", "Value of FHIR resource was not of type Coding"));
    }

    public static ComponentKeyPair ofFailedTooManyValues(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, new HashableCoding("http://fhir-evaluator/strat/system", "fail-too-many-values", "Expected one value, but found more"));
    }

    public static ComponentKeyPair ofFailedNoValueFound(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, new HashableCoding("http://fhir-evaluator/strat/system", "fail-no-value-found", "Expected one value, but found none"));
    }

    public static ComponentKeyPair ofFailedMissingFields(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, new HashableCoding("http://fhir-evaluator/strat/system", "fail-missing-fields", "Coding was missing system or code"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ComponentKeyPair that = (ComponentKeyPair) o;
        return this.definitionCode.equals(that.definitionCode) && this.valueCode.equals(that.valueCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitionCode, valueCode);
    }

    public MeasureReport.StratifierGroupComponentComponent toReport() {
        return new MeasureReport.StratifierGroupComponentComponent()
                .setCode(new CodeableConcept(definitionCode.toCoding()))
                .setValue(new CodeableConcept(valueCode.toCoding()));
    }

}
