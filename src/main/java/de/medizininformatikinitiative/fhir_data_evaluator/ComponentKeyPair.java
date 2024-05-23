package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.Objects;

/**
 * Represents a found value of a resource.
 *
 * @param definitionCode the code of the stratifier component
 * @param valueCode the value found in the resource
 */
public record ComponentKeyPair(ComponentKey definitionCode, ComponentKey valueCode) {

    public static ComponentKeyPair ofFailedInvalidType(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, HashableCoding.FAIL_INVALID_TYPE);
    }

    public static ComponentKeyPair ofFailedTooManyValues(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, HashableCoding.FAIL_TOO_MANY_VALUES);
    }

    public static ComponentKeyPair ofFailedNoValueFound(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, HashableCoding.FAIL_NO_VALUE_FOUND);
    }

    public static ComponentKeyPair ofFailedMissingFields(ComponentKey definitionCode) {
        return new ComponentKeyPair(definitionCode, HashableCoding.FAIL_MISSING_FIELDS);
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
                .setCode(definitionCode.toCodeableConcept())
                .setValue(valueCode.toCodeableConcept());
    }

}
