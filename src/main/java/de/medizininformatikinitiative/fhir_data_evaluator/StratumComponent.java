package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import static java.util.Objects.requireNonNull;

/**
 * Represents a found value of a resource.
 *
 * @param code  the code of the stratifier component
 * @param value the value found in the resource
 */
public record StratumComponent(HashableCoding code, HashableCoding value) {

    public StratumComponent {
        requireNonNull(code);
        requireNonNull(value);
    }

    public static StratumComponent ofFailedInvalidType(HashableCoding code) {
        return new StratumComponent(code, HashableCoding.FAIL_INVALID_TYPE);
    }

    public static StratumComponent ofFailedTooManyValues(HashableCoding code) {
        return new StratumComponent(code, HashableCoding.FAIL_TOO_MANY_VALUES);
    }

    public static StratumComponent ofFailedNoValueFound(HashableCoding code) {
        return new StratumComponent(code, HashableCoding.FAIL_NO_VALUE_FOUND);
    }

    public static StratumComponent ofFailedMissingFields(HashableCoding code) {
        return new StratumComponent(code, HashableCoding.FAIL_MISSING_FIELDS);
    }

    public MeasureReport.StratifierGroupComponentComponent toReport() {
        return new MeasureReport.StratifierGroupComponentComponent()
                .setCode(code.toCodeableConcept())
                .setValue(value.toCodeableConcept());
    }
}
