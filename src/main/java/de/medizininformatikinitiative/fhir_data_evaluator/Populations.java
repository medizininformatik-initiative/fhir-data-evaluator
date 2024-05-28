package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Represents possibly multiple populations either on group or on stratifier level.
 * <p>
 * Currently, the only accepted population is the {@link InitialPopulation}.
 *
 * @param initialPopulation the initial population
 */
public record Populations(InitialPopulation initialPopulation) {

    public static final Populations ZERO = new Populations(InitialPopulation.ZERO);
    public static final Populations ONE = new Populations(InitialPopulation.ONE);

    public Populations {
        requireNonNull(initialPopulation);
    }

    public Populations increaseCounts() {
        return new Populations(initialPopulation.increaseCount());
    }

    public Populations merge(Populations other) {
        return new Populations(initialPopulation.merge(other.initialPopulation));
    }

    public List<MeasureReport.MeasureReportGroupPopulationComponent> toReportGroupPopulations() {
        return List.of(initialPopulation.toReportGroupPopulation());
    }

    public List<MeasureReport.StratifierGroupPopulationComponent> toReportStratifierPopulations() {
        return List.of(initialPopulation.toReportStratifierPopulation());
    }
}
