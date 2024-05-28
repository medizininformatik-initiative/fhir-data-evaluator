package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;

/**
 * Counts the initial population of a group either on group-level or on stratifier-level.
 */
public record InitialPopulation(int count) {


    public MeasureReport.StratifierGroupPopulationComponent toReportStratifierPopulation() {
        return new MeasureReport.StratifierGroupPopulationComponent()
                .setCode(INITIAL_POPULATION_CODING.toCodeableConcept())
                .setCount(count);
    }

    public MeasureReport.MeasureReportGroupPopulationComponent toReportGroupPopulation() {
        return new MeasureReport.MeasureReportGroupPopulationComponent()
                .setCode(INITIAL_POPULATION_CODING.toCodeableConcept())
                .setCount(count);
    }

    public InitialPopulation merge(InitialPopulation other) {
        return new InitialPopulation(count + other.count);
    }
}
