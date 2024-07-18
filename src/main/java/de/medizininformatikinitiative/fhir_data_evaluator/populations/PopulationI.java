package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.MeasureReport;

/**
 * Interface between the different types (or combinations) of populations.
 * <p>
 * A group must have an initial population, but it might or might not have a measure population and a measure observation
 * population. This leads to different possible combinations of population types inside a collection of populations.
 * To guarantee type safety, for each allowed (according to the profile of the continuous-variable measure) combination
 * there is a different implementation of this interface.
 *
 * @param <T>   the type of the population
 */
public interface PopulationI<T extends PopulationI<T>> {

    T merge(T population);

    MeasureReport.StratifierGroupComponent toReportStratifierGroupComponent();
    MeasureReport.MeasureReportGroupComponent toReportGroupComponent();

}
