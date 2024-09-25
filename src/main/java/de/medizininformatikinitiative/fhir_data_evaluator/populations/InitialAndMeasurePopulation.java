package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.Individual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialAndMeasureIndividual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialIndividual;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

/**
 * Represents a collection of populations containing the initial population and measure population without the measure observation
 * population.
 * <p>
 * This collection of populations is used on group level and on stratifier level.
 *
 * @param initialPopulation the initial population
 * @param measurePopulation the measure population
 */
public record InitialAndMeasurePopulation(InitialPopulation initialPopulation, MeasurePopulation measurePopulation)
        implements Population<InitialAndMeasurePopulation, InitialAndMeasureIndividual> {
    public static InitialAndMeasurePopulation ZERO = new InitialAndMeasurePopulation(InitialPopulation.ZERO,
            MeasurePopulation.ZERO);

    /**
     * Increments the count of the initial population and the measure population.
     *
     * @param individual the {@link Individual} used to increment the initial population and the measure population
     */
    @Override
    public InitialAndMeasurePopulation increment(InitialAndMeasureIndividual individual) {
        return new InitialAndMeasurePopulation(
                initialPopulation.increment(InitialIndividual.INSTANCE),
                individual.containsMeasurePop() ? measurePopulation.increment() : measurePopulation);
    }

    @Override
    public MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent() {
        return new MeasureReport.StratifierGroupComponent().setPopulation(
                List.of(initialPopulation.toReportStratifierPopulation(),
                        measurePopulation.toReportStratifierPopulation()));
    }

    @Override
    public MeasureReport.MeasureReportGroupComponent toReportGroupComponent() {
        return new MeasureReport.MeasureReportGroupComponent().setPopulation(
                List.of(initialPopulation.toReportGroupPopulation(),
                        measurePopulation.toReportGroupPopulation())
        );
    }
}
