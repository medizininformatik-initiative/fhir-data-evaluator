package de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;

/**
 * Represents an individual of an initial population.
 * <p>
 * This does not hold any data because an initial population is always incremented by 1.
 */
public enum InitialIndividual implements Individual<InitialPopulation> {
    INSTANCE;

    public int count() {
        return 1;
    }

    @Override
    public InitialPopulation toPopulation() {
        return InitialPopulation.ONE;
    }
}
