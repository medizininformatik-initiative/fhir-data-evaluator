package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;

/**
 * Counts the populations of a group either on group-level or on stratifier-level.
 * <p>
 * Currently, the only accepted population is the Initial-Population.
 */
public record Populations(InitialPopulation initialPopulation) {
    public static final Populations INITIAL_ONE = new Populations(new InitialPopulation(1));
    public static final Populations INITIAL_ZERO = new Populations(new InitialPopulation(0));


    public Populations increaseCount() {
        return new Populations(new InitialPopulation(initialPopulation().count() + 1));
    }

    public Populations merge(Populations other) {
        return new Populations(initialPopulation.merge(other.initialPopulation));
    }

    public List<MeasureReport.StratifierGroupPopulationComponent> toReportStratifierPopulations() {
        return List.of(initialPopulation.toReportStratifierPopulation());
    }

    public List<MeasureReport.MeasureReportGroupPopulationComponent> toReportGroupPopulations() {
        return List.of(initialPopulation.toReportGroupPopulation());
    }
}
