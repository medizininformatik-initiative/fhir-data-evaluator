package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.MeasureReport;

/**
 * A Population represents one or more Populations.
 * <p>
 * A group must have an initial population, but it might or might not have a measure population and a measure observation
 * population. This leads to different possible combinations of population types inside a collection of populations.
 * To guarantee type safety, for each allowed (according to the profile of the continuous-variable measure) combination
 * there is a different implementation of this interface.
 *
 * @param <T>   the type of the population
 */
public interface Population<T extends Population<T>> {

    /**
     * Merges all populations of two populations.
     *
     * @param population    the population to merge into the current population
     * @return  the new population containing the merged populations
     */
    T merge(T population);

    MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent();
    MeasureReport.MeasureReportGroupComponent toReportGroupComponent();

}
