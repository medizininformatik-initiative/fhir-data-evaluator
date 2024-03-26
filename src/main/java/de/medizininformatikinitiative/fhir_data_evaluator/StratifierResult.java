package de.medizininformatikinitiative.fhir_data_evaluator;

public interface StratifierResult {

    StratifierResult merge(StratifierResult other);
}
