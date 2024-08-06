package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;

import java.util.List;

/**
 * Represents a collection of populations containing the initial population, measure population and measure observation
 * population.
 * <p>
 * This collection of populations is used on group level and on stratifier level.
 *
 * @param initialPopulation     the initial population
 * @param measurePopulation     the measure population
 * @param observationPopulation the measure observation population
 */
public record InitialAndMeasureAndObsPopulation(InitialPopulation initialPopulation,
                                                MeasurePopulation measurePopulation,
                                                ObservationPopulation observationPopulation)
        implements Population<InitialAndMeasureAndObsPopulation> {

    public static InitialAndMeasureAndObsPopulation empty() {
        return new InitialAndMeasureAndObsPopulation(InitialPopulation.ZERO, MeasurePopulation.ZERO, ObservationPopulation.empty());
    }

    @Override
    public InitialAndMeasureAndObsPopulation merge(InitialAndMeasureAndObsPopulation other) {
        return new InitialAndMeasureAndObsPopulation(
                initialPopulation.merge(other.initialPopulation),
                measurePopulation.merge(other.measurePopulation),
                observationPopulation.merge(other.observationPopulation)
        );
    }

    @Override
    public MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent() {
        return new MeasureReport.StratifierGroupComponent()
                .setPopulation(
                        List.of(initialPopulation.toReportStratifierPopulation(),
                                measurePopulation.toReportStratifierPopulation(),
                                observationPopulation.toReportStratifierPopulation())
                )
                .setMeasureScore(new Quantity(observationPopulation.aggregateMethod().getScore()));
    }

    @Override
    public MeasureReport.MeasureReportGroupComponent toReportGroupComponent() {
        return new MeasureReport.MeasureReportGroupComponent()
                .setPopulation(
                        List.of(initialPopulation.toReportGroupPopulation(),
                                measurePopulation.toReportGroupPopulation(),
                                observationPopulation.toReportGroupPopulation())
                )
                .setMeasureScore(new Quantity(observationPopulation.aggregateMethod().getScore()));
    }
}
