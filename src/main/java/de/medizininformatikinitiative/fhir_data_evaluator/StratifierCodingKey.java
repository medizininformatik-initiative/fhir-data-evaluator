package de.medizininformatikinitiative.fhir_data_evaluator;

import java.util.Objects;

public record StratifierCodingKey(HashableCoding definitionCode, HashableCoding valueCode) {

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StratifierCodingKey that = (StratifierCodingKey) o;
        return this.definitionCode.equals(that.definitionCode) && this.valueCode.equals(that.valueCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitionCode.code(), definitionCode.system(), valueCode.code(), valueCode.system());
    }

}
