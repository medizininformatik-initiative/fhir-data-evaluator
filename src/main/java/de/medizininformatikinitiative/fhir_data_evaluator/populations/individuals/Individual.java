package de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.Population;

/**
 * Immutable class that holds values that are used to increment an existing {@link Population}.
 * <p>
 * Incrementing here means to increment by a single step from a resource evaluation, for example adding 1 to a count. So
 * this class represents one individual of a population.
 *
 * @param <P> the type of the population that is incremented
 */
public interface Individual<P extends Population<P, ? extends Individual<P>>> {

    /**
     * Creates a {@link Population} from an individual that is used as a new initial value in the populations map of a
     * {@link de.medizininformatikinitiative.fhir_data_evaluator.StratifierResult}.
     */
    P toPopulation();
}
