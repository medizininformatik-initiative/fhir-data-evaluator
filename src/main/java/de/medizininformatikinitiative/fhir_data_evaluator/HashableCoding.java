package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

import java.util.Objects;

/**
 * Represents a {@link Coding} with an implementation of {@link #equals(Object)} and {@link #hashCode()}.
 */
public record HashableCoding(String system, String code, String display) {
    public static final HashableCoding FAIL_NO_VALUE_FOUND = new HashableCoding("http://fhir-evaluator/strat/system", "fail-no-value-found", "Expected one value, but found none");
    public static final HashableCoding FAIL_TOO_MANY_VALUES = new HashableCoding("http://fhir-evaluator/strat/system", "fail-too-many-values", "Expected one value, but found more");
    public static final HashableCoding FAIL_INVALID_TYPE = new HashableCoding("http://fhir-evaluator/strat/system", "fail-invalid-type", "Value of FHIR resource was not of type Coding");
    public static final HashableCoding FAIL_MISSING_FIELDS = new HashableCoding("http://fhir-evaluator/strat/system", "fail-missing-fields", "Coding was missing system or code");
    public static final HashableCoding INITIAL_POPULATION_CODING = new HashableCoding("http://terminology.hl7.org/CodeSystem/measure-population", "initial-population", "display");

    static HashableCoding ofFhirCoding(Coding coding) {
        return new HashableCoding(coding.getSystem(), coding.getCode(), coding.getDisplay());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        HashableCoding that = (HashableCoding) o;
        return this.system.equals(that.system) && this.code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, code);
    }

    public Coding toCoding() {
        return new Coding().setSystem(system).setCode(code).setDisplay(display);
    }

    public CodeableConcept toCodeableConcept() {
        return new CodeableConcept(toCoding());
    }
}
