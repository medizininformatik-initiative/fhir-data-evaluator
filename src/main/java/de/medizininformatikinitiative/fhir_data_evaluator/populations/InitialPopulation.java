package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.Individual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialIndividual;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;

/**
 * Represents an initial population either on group or on stratifier level.
 *
 * @param count the number of members in the initial population
 */
public record InitialPopulation(int count) implements Population<InitialPopulation, InitialIndividual> {

    public static final InitialPopulation ZERO = new InitialPopulation(0);
    public static final InitialPopulation ONE = new InitialPopulation(1);

    /**
     * Increments the count of the initial population.
     *
     * @param individual the {@link Individual} used to increment the initial population
     */
    @Override
    public InitialPopulation increment(InitialIndividual individual) {
        return new InitialPopulation(count + individual.count());
    }

    @Override
    public MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent() {
        return new MeasureReport.StratifierGroupComponent().setPopulation(List.of(toReportStratifierPopulation()));
    }

    @Override
    public MeasureReport.MeasureReportGroupComponent toReportGroupComponent() {
        return new MeasureReport.MeasureReportGroupComponent().setPopulation(List.of(toReportGroupPopulation()));
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
