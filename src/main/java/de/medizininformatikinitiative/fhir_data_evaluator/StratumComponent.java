package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.Objects;

/**
 * Represents a found value of a resource.
 *
 * @param code the code of the stratifier component
 * @param value the value found in the resource
 */
public record StratumComponent(HashableCoding code, HashableCoding value) {

    public static StratumComponent ofFailedInvalidType(HashableCoding definitionCode) {
        return new StratumComponent(definitionCode, HashableCoding.FAIL_INVALID_TYPE);
    }

    public static StratumComponent ofFailedTooManyValues(HashableCoding definitionCode) {
        return new StratumComponent(definitionCode, HashableCoding.FAIL_TOO_MANY_VALUES);
    }

    public static StratumComponent ofFailedNoValueFound(HashableCoding definitionCode) {
        return new StratumComponent(definitionCode, HashableCoding.FAIL_NO_VALUE_FOUND);
    }

    public static StratumComponent ofFailedMissingFields(HashableCoding definitionCode) {
        return new StratumComponent(definitionCode, HashableCoding.FAIL_MISSING_FIELDS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StratumComponent that = (StratumComponent) o;
        return this.code.equals(that.code) && this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, value);
    }

    public MeasureReport.StratifierGroupComponentComponent toReport() {
        return new MeasureReport.StratifierGroupComponentComponent()
                .setCode(code.toCodeableConcept())
                .setValue(value.toCodeableConcept());
    }

}
