package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialAndMeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;
import org.hl7.fhir.r4.model.ExpressionNode;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

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
public record GroupReduceOpMeasure(List<StratifierReduceOp<InitialAndMeasurePopulation>> stratifierReduceOps,
                                   ExpressionNode measurePopulationExpression,
                                   FHIRPathEngine fhirPathEngine)
        implements BiFunction<GroupResult<InitialAndMeasurePopulation>, Resource, GroupResult<InitialAndMeasurePopulation>> {

    public GroupReduceOpMeasure {
        requireNonNull(stratifierReduceOps);
        requireNonNull(measurePopulationExpression);
        requireNonNull(fhirPathEngine);
    }

    @Override
    public GroupResult<InitialAndMeasurePopulation> apply(GroupResult<InitialAndMeasurePopulation> groupResult, Resource resource) {
        return groupResult.applyResource(stratifierReduceOps, resource, calcIncrementPopulation(resource));
    }

    private InitialAndMeasurePopulation calcIncrementPopulation(Resource resource) {
        Optional<Resource> measurePopResource = MeasurePopulation.evaluateMeasurePopResource(resource, measurePopulationExpression, fhirPathEngine);
        var evaluatedMeasurePop = measurePopResource.isPresent() ? MeasurePopulation.ONE : MeasurePopulation.ZERO;

        return new InitialAndMeasurePopulation(InitialPopulation.ONE, evaluatedMeasurePop);
    }
}
