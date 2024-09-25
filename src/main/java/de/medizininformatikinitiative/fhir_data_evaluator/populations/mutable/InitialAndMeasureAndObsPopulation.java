package de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.Population;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.Individual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialAndMeasureAndObsIndividual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialIndividual;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

/**
 * Represents a collection of populations containing the initial population, measure population and measure observation
 * population.
 * <p>
 * This collection of populations is used on group level and on stratifier level.
 * <p>
 * This record by itself is not mutable, but it holds a mutable {@link AggregateUniqueCounter}.
 *
 * @param initialPopulation     the initial population
 * @param measurePopulation     the measure population
 * @param observationPopulation the measure observation population
 */
public record InitialAndMeasureAndObsPopulation(InitialPopulation initialPopulation,
                                                MeasurePopulation measurePopulation,
                                                ObservationPopulation observationPopulation)
        implements Population<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual> {

    public static InitialAndMeasureAndObsPopulation empty() {
        return new InitialAndMeasureAndObsPopulation(InitialPopulation.ZERO, MeasurePopulation.ZERO, ObservationPopulation.empty());
    }


    /**
     * Increments the count of the initial population, measure population and observation population and might add a
     * value to the observation population if present in the {@code individual}.
     *
     * @param individual the {@link Individual} used to increment the initial population, measure population and
     *                   observation population
     */
    @Override
    public InitialAndMeasureAndObsPopulation increment(InitialAndMeasureAndObsIndividual individual) {
        return new InitialAndMeasureAndObsPopulation(
                initialPopulation.increment(InitialIndividual.INSTANCE),
                individual.containsMeasurePop() ? measurePopulation.increment() : measurePopulation,
                individual.obsValue().map(observationPopulation::increment).orElse(observationPopulation));
    }

    @Override
    public MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent() {
        return new MeasureReport.StratifierGroupComponent()
                .setPopulation(
                        List.of(initialPopulation.toReportStratifierPopulation(),
                                measurePopulation.toReportStratifierPopulation(),
                                observationPopulation.toReportStratifierPopulation())
                )
                .setMeasureScore(observationPopulation.aggregateMethod().score());
    }

    @Override
    public MeasureReport.MeasureReportGroupComponent toReportGroupComponent() {
        return new MeasureReport.MeasureReportGroupComponent()
                .setPopulation(
                        List.of(initialPopulation.toReportGroupPopulation(),
                                measurePopulation.toReportGroupPopulation(),
                                observationPopulation.toReportGroupPopulation())
                )
                .setMeasureScore(observationPopulation.aggregateMethod().score());
    }
}
