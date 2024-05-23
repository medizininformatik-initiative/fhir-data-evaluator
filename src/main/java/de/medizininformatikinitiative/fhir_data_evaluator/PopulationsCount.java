package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;

/**
 * Counts the populations of the Measure either on group-level or on stratifier-level.
 * <p>
 * Currently, the only accepted population is the Initial-Population.
 */
public record PopulationsCount(PopulationCount initialPopulation) {
    public static final PopulationsCount INITIAL_ONE = new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1));
    public static final PopulationsCount INITIAL_ZERO = new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 0));


    public PopulationsCount increaseCount() {
        return new PopulationsCount(new PopulationCount(this.initialPopulation().code(), initialPopulation().count() + 1));
    }

    public PopulationsCount merge(PopulationsCount other) {
        return new PopulationsCount(initialPopulation.merge(other.initialPopulation));
    }

    public List<MeasureReport.StratifierGroupPopulationComponent> toReportStratifierPopulations() {
        return List.of(initialPopulation.toReportStratifierPopulation());
    }

    public List<MeasureReport.MeasureReportGroupPopulationComponent> toReportGroupPopulations() {
        return List.of(initialPopulation.toReportGroupPopulation());
    }
}
