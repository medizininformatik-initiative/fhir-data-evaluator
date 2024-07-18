package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.LinkedList;

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
        implements PopulationI<InitialAndMeasurePopulation> {
    public static InitialAndMeasurePopulation ZERO = new InitialAndMeasurePopulation(InitialPopulation.ZERO,
            MeasurePopulation.ZERO);

    @Override
    public InitialAndMeasurePopulation merge(InitialAndMeasurePopulation other) {
        return new InitialAndMeasurePopulation(
                initialPopulation.merge(other.initialPopulation),
                measurePopulation.merge(other.measurePopulation));
    }

    @Override
    public MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent() {
        var populations = new LinkedList<MeasureReport.StratifierGroupPopulationComponent>();

        populations.add(initialPopulation.toReportStratifierPopulation());
        populations.add(measurePopulation.toReportStratifierPopulation());

        return new MeasureReport.StratifierGroupComponent().setPopulation(populations);
    }

    @Override
    public MeasureReport.MeasureReportGroupComponent toReportGroupComponent() {
        var populations = new LinkedList<MeasureReport.MeasureReportGroupPopulationComponent>();

        populations.add(initialPopulation.toReportGroupPopulation());
        populations.add(measurePopulation.toReportGroupPopulation());

        return new MeasureReport.MeasureReportGroupComponent().setPopulation(populations);
    }
}
