package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

public record PopulationCount(HashableCoding code, int count) {

    public MeasureReport.StratifierGroupPopulationComponent toReportStratifierPopulation() {
        return new MeasureReport.StratifierGroupPopulationComponent()
                .setCode(code.toCodeableConcept())
                .setCount(count);
    }

    public MeasureReport.MeasureReportGroupPopulationComponent toReportGroupPopulation() {
        return new MeasureReport.MeasureReportGroupPopulationComponent()
                .setCode(code.toCodeableConcept())
                .setCount(count);
    }

    public PopulationCount merge(PopulationCount other) {
        assert code.equals(other.code);
        return new PopulationCount(code, count + other.count);
    }
}
