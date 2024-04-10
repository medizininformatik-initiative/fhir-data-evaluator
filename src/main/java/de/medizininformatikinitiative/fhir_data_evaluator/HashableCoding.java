package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Coding;

import java.util.Objects;

public record HashableCoding(String system, String code, String display) implements ComponentKey {
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

    @Override
    public Coding toCoding() {
        return new Coding().setSystem(system).setCode(code);
    }
}
