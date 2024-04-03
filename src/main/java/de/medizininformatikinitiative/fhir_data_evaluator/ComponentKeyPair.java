package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.Objects;

public record ComponentKeyPair(ComponentKey definitionCode, ComponentKey valueCode) {

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
