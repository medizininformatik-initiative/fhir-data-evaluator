package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialAndMeasureAndObsPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.ObservationPopulation;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.ExpressionNode;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
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
 * This operates on GroupResults that contain an {@link InitialAndMeasureAndObsPopulation}.
 *
 * @param stratifierReduceOps             holds one {@link StratifierReduceOp} for each stratifier in a group
 * @param measurePopulationExpression     the expression to evaluate the measure population
 * @param observationPopulationExpression the expression to evaluate the observation population
 */
public record GroupReduceOpObservation(List<StratifierReduceOp<InitialAndMeasureAndObsPopulation>> stratifierReduceOps,
                                       ExpressionNode measurePopulationExpression,
                                       ExpressionNode observationPopulationExpression, FHIRPathEngine fhirPathEngine)
        implements BiFunction<GroupResult<InitialAndMeasureAndObsPopulation>, Resource, GroupResult<InitialAndMeasureAndObsPopulation>> {

    public GroupReduceOpObservation {
        requireNonNull(stratifierReduceOps);
        requireNonNull(measurePopulationExpression);
        requireNonNull(observationPopulationExpression);
        requireNonNull(fhirPathEngine);
    }

    @Override
    public GroupResult<InitialAndMeasureAndObsPopulation> apply(GroupResult<InitialAndMeasureAndObsPopulation> groupResult, Resource resource) {
        return groupResult.applyResource(stratifierReduceOps, resource, calcIncrementPopulation(resource));
    }

    private InitialAndMeasureAndObsPopulation calcIncrementPopulation(Resource resource) {

        Optional<Resource> measurePopResource = MeasurePopulation.evaluateMeasurePopResource(resource, measurePopulationExpression, fhirPathEngine);
        var evaluatedMeasurePop = measurePopResource.isPresent() ? MeasurePopulation.ONE : MeasurePopulation.ZERO;
        var evaluatedObsPop = measurePopResource
                .map(r -> evaluateObservationPop(r, observationPopulationExpression))
                .orElse(ObservationPopulation.empty());

        return new InitialAndMeasureAndObsPopulation(InitialPopulation.ONE, evaluatedMeasurePop, evaluatedObsPop);
    }

    public ObservationPopulation evaluateObservationPop(Resource resource, ExpressionNode expression) {
        Optional<String> value = evaluateObservationPopResource(resource, expression);

        return value.map(ObservationPopulation::initialWithValue).orElse(ObservationPopulation.empty());
    }

    private Optional<String> evaluateObservationPopResource(Resource resource, ExpressionNode expression) {
        List<Base> found = fhirPathEngine.evaluate(resource, expression);

        if (found.isEmpty())
            return Optional.empty();

        if (found.size() > 1)
            throw new IllegalArgumentException("Measure observation population evaluated into more than one entity");

        if (found.get(0) instanceof StringType s)
            return Optional.of(s.getValue());

        throw new IllegalArgumentException("Measure observation population evaluated into different type than 'String'");
    }
}
