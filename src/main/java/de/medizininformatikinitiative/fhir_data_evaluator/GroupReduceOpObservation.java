package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.MeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialAndMeasureAndObsIndividual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable.InitialAndMeasureAndObsPopulation;
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
public record GroupReduceOpObservation(
        List<StratifierReduceOp<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>> stratifierReduceOps,
        ExpressionNode measurePopulationExpression,
        ExpressionNode observationPopulationExpression,
        FHIRPathEngine fhirPathEngine)
        implements BiFunction<GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>,
        Resource,
        GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>> {

    public GroupReduceOpObservation {
        requireNonNull(stratifierReduceOps);
        requireNonNull(measurePopulationExpression);
        requireNonNull(observationPopulationExpression);
        requireNonNull(fhirPathEngine);
    }

    @Override
    public GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual> apply(
            GroupResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual> groupResult,
            Resource resource) {
        return groupResult.applyResource(stratifierReduceOps, resource, calcIncrementIndividual(resource));
    }

    private InitialAndMeasureAndObsIndividual calcIncrementIndividual(Resource resource) {
        Optional<Resource> measurePopResource = MeasurePopulation.evaluateMeasurePopResource(resource, measurePopulationExpression,
                fhirPathEngine);
        var obsVal = measurePopResource.flatMap(r -> evaluateObservationPop(r, observationPopulationExpression));

        return new InitialAndMeasureAndObsIndividual(measurePopResource.isPresent(), obsVal);
    }

    private Optional<String> evaluateObservationPop(Resource resource, ExpressionNode expression) {
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
