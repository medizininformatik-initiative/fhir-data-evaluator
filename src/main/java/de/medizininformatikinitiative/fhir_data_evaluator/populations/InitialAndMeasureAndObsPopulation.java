package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;

import java.util.LinkedList;

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
        implements PopulationI<InitialAndMeasureAndObsPopulation> {

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
        var populations = new LinkedList<MeasureReport.StratifierGroupPopulationComponent>();

        populations.add(initialPopulation.toReportStratifierPopulation());
        populations.add(measurePopulation.toReportStratifierPopulation());
        populations.add(observationPopulation.toReportStratifierPopulation());

        return new MeasureReport.StratifierGroupComponent()
                .setPopulation(populations)
                .setMeasureScore(new Quantity(observationPopulation.aggregateMethod().getScore()));
    }

    @Override
    public MeasureReport.MeasureReportGroupComponent toReportGroupComponent() {
        var populations = new LinkedList<MeasureReport.MeasureReportGroupPopulationComponent>();

        populations.add(initialPopulation.toReportGroupPopulation());
        populations.add(measurePopulation.toReportGroupPopulation());
        populations.add(observationPopulation.toReportGroupPopulation());

        return new MeasureReport.MeasureReportGroupComponent()
                .setPopulation(populations)
                .setMeasureScore(new Quantity(observationPopulation.aggregateMethod().getScore()));
    }
}
