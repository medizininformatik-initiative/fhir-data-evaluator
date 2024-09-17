package de.medizininformatikinitiative.fhir_data_evaluator.populations;

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
        implements Population<InitialAndMeasurePopulation> {
    public static InitialAndMeasurePopulation ZERO = new InitialAndMeasurePopulation(InitialPopulation.ZERO,
            MeasurePopulation.ZERO);

    @Override
    public InitialAndMeasurePopulation merge(InitialAndMeasurePopulation other) {
        return new InitialAndMeasurePopulation(
                initialPopulation.merge(other.initialPopulation),
                measurePopulation.merge(other.measurePopulation));
    }

    @Override
    public InitialAndMeasurePopulation deepCopy() {
        return new InitialAndMeasurePopulation(initialPopulation.deepCopy(), measurePopulation.deepCopy());
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
