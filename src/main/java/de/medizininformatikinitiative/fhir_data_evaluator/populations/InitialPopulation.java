package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.LinkedList;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;

/**
 * Represents an initial population either on group or on stratifier level.
 *
 * @param count the number of members in the initial population
 */
public record InitialPopulation(int count) implements PopulationI<InitialPopulation> {

    public static final InitialPopulation ZERO = new InitialPopulation(0);
    public static final InitialPopulation ONE = new InitialPopulation(1);


    public InitialPopulation merge(InitialPopulation other) {
        return new InitialPopulation(count + other.count);
    }

    @Override
    public MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent() {
        var populations = new LinkedList<MeasureReport.StratifierGroupPopulationComponent>();

        populations.add(toReportStratifierPopulation());

        return new MeasureReport.StratifierGroupComponent().setPopulation(populations);
    }

    @Override
    public MeasureReport.MeasureReportGroupComponent toReportGroupComponent() {
        var populations = new LinkedList<MeasureReport.MeasureReportGroupPopulationComponent>();

        populations.add(toReportGroupPopulation());

        return new MeasureReport.MeasureReportGroupComponent().setPopulation(populations);
    }

    public MeasureReport.MeasureReportGroupPopulationComponent toReportGroupPopulation() {
        return new MeasureReport.MeasureReportGroupPopulationComponent()
                .setCode(INITIAL_POPULATION_CODING.toCodeableConcept())
                .setCount(count);
    }

    public MeasureReport.StratifierGroupPopulationComponent toReportStratifierPopulation() {
        return new MeasureReport.StratifierGroupPopulationComponent()
                .setCode(INITIAL_POPULATION_CODING.toCodeableConcept())
                .setCount(count);
    }
}
