package de.medizininformatikinitiative.fhir_data_evaluator;

import java.util.Objects;

public record HashableCoding(String system, String code) {

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
}
