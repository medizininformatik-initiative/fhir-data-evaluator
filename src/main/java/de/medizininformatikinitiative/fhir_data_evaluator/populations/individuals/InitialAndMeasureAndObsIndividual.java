package de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable.InitialAndMeasureAndObsPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable.ObservationPopulation;

import java.util.Optional;

/**
 * Represents an individual of an initial population, a measure population and an observation population.
 *
 * @param containsMeasurePop whether the measure population is part of the individual and thus should be incremented by
 *                           one
 * @param obsValue           the value that might be added to the aggregate method of an observation population
 */
public record InitialAndMeasureAndObsIndividual(boolean containsMeasurePop,
                                                Optional<String> obsValue) implements Individual<InitialAndMeasureAndObsPopulation> {

    @Override
    public InitialAndMeasureAndObsPopulation toPopulation() {
        return new InitialAndMeasureAndObsPopulation(
                InitialPopulation.ONE,
                containsMeasurePop ? MeasurePopulation.ONE : MeasurePopulation.ZERO,
                obsValue.map(ObservationPopulation::initialWithValue).orElse(ObservationPopulation.empty()));
    }
}
