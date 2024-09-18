package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.Individual;
import org.hl7.fhir.r4.model.MeasureReport;

/**
 * A Population represents one or more Populations.
 * <p>
 * A group must have an initial population, but it might or might not have a measure population and a measure observation
 * population. This leads to different possible combinations of population types inside a collection of populations.
 * To guarantee type safety, for each allowed (according to the profile of the continuous-variable measure) combination
 * there is a different implementation of this interface.
 *
 * @param <T> the type of the population
 * @param <I> the corresponding individual of the population type, which is used to increment the population
 */
public interface Population<T extends Population<T, I>, I extends Individual<T>> {

    /**
     * Adds the values of the {@link Individual} to this population.
     *
     * @param individual the {@link Individual} used to increment the current population
     * @return the new population containing the data of both the old population and the individual
     */
    T increment(I individual);

    MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent();

    MeasureReport.MeasureReportGroupComponent toReportGroupComponent();

}
