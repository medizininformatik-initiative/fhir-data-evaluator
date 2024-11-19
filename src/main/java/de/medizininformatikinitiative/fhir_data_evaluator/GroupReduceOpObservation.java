package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.fhirpath.IFhirPath;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialAndMeasureAndObsIndividual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable.InitialAndMeasureAndObsPopulation;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable.ObservationPopulation.evaluateObservationPop;
import static java.util.Objects.requireNonNull;

/**
 * An operator that appends the data of a {@link Resource} to a {@link GroupResult} producing a new {@code GroupResult}.
 * <p>
 * Applying a {@code GroupReduceOp} to a {@code GroupResult} and a {@code Resource} evaluates the
 * {@link GroupResult#populations() GroupResult populations} with the {@code Resource} and applies the {@code Resource}
 * to each {@link StratifierResult} in the {@code GroupResult}.
 * <p>
 * This operates on GroupResults that contain an {@link InitialAndMeasureAndObsPopulation}.
 *
 * @param stratifierReduceOps             holds one {@link StratifierReduceOp} for each stratifier in a group
 * @param measurePopulationExpression     the expression to evaluate the measure population
 * @param observationPopulationExpression the expression to evaluate the observation population
 */
public record GroupReduceOpObservation(
        List<StratifierReduceOp<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>> stratifierReduceOps,
        IFhirPath.IParsedExpression measurePopulationExpression,
        IFhirPath.IParsedExpression observationPopulationExpression)
        implements BiFunction<GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>,
        ResourceWithIncludes,
        GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>> {

    public GroupReduceOpObservation {
        requireNonNull(stratifierReduceOps);
        requireNonNull(measurePopulationExpression);
        requireNonNull(observationPopulationExpression);
    }

    @Override
    public GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual> apply(
            GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual> groupResult,
            ResourceWithIncludes resource) {
        return groupResult.applyResource(stratifierReduceOps, resource, calcIncrementIndividual(resource));
    }

    private InitialAndMeasureAndObsIndividual calcIncrementIndividual(ResourceWithIncludes resource) {
        Optional<ResourceWithIncludes> measurePopResource = MeasurePopulation.evaluateMeasurePopResource(resource, measurePopulationExpression);
        var obsVal = measurePopResource.flatMap(r -> evaluateObservationPop(r, observationPopulationExpression));

        return new InitialAndMeasureAndObsIndividual(measurePopResource.isPresent(), obsVal);
    }


}
