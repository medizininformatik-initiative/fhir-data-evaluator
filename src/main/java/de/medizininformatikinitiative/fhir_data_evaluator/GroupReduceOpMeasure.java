package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.fhirpath.IFhirPath;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialAndMeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialAndMeasureIndividual;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * An operator that appends the data of a {@link Resource} to a {@link GroupResult} producing a new {@code GroupResult}.
 * <p>
 * Applying a {@code GroupReduceOp} to a {@code GroupResult} and a {@code Resource} evaluates the
 * {@link GroupResult#populations() GroupResult populations} with the {@code Resource} and applies the {@code Resource}
 * to each {@link StratifierResult} in the {@code GroupResult}.
 * <p>
 * This operates on GroupResults that contain an {@link InitialAndMeasurePopulation}.
 *
 * @param stratifierReduceOps         holds one {@link StratifierReduceOp} for each stratifier in a group
 * @param measurePopulationExpression the expression to evaluate the measure population
 */
public record GroupReduceOpMeasure(
        List<StratifierReduceOp<InitialAndMeasurePopulation, InitialAndMeasureIndividual>> stratifierReduceOps,
        IFhirPath.IParsedExpression measurePopulationExpression)
        implements BiFunction<GroupResult<InitialAndMeasurePopulation, InitialAndMeasureIndividual>, ResourceWithIncludes,
        GroupResult<InitialAndMeasurePopulation, InitialAndMeasureIndividual>> {

    public GroupReduceOpMeasure {
        requireNonNull(stratifierReduceOps);
        requireNonNull(measurePopulationExpression);
    }

    @Override
    public GroupResult<InitialAndMeasurePopulation, InitialAndMeasureIndividual> apply(
            GroupResult<InitialAndMeasurePopulation, InitialAndMeasureIndividual> groupResult,
            ResourceWithIncludes resource) {
        return groupResult.applyResource(stratifierReduceOps, resource, calcIncrementIndividual(resource));
    }

    private InitialAndMeasureIndividual calcIncrementIndividual(ResourceWithIncludes resource) {
        Optional<ResourceWithIncludes> measurePopResource = MeasurePopulation.evaluateMeasurePopResource(resource, measurePopulationExpression);

        return new InitialAndMeasureIndividual(measurePopResource.isPresent());
    }
}
