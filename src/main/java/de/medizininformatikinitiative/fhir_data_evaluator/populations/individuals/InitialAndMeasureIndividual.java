package de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialAndMeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;

/**
 * Represents an individual of an initial population and a measure population.
 *
 * @param containsMeasurePop whether the measure population is part of the individual and thus should be incremented by
 *                           one
 */
public record InitialAndMeasureIndividual(
        boolean containsMeasurePop) implements Individual<InitialAndMeasurePopulation> {

    @Override
    public InitialAndMeasurePopulation toPopulation() {
        return new InitialAndMeasurePopulation(
                InitialPopulation.ONE,
                containsMeasurePop ? MeasurePopulation.ONE : MeasurePopulation.ZERO);
    }
}
