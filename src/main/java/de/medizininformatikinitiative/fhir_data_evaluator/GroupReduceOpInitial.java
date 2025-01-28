package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialIndividual;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * An operator that appends the data of a {@link Resource} to a {@link GroupResult} producing a new {@code GroupResult}.
 * <p>
 * Applying a {@code GroupReduceOp} to a {@code GroupResult} and a {@code Resource} evaluates the
 * {@link GroupResult#populations() GroupResult populations} with the {@code Resource} and applies the {@code Resource}
 * to each {@link StratifierResult} in the {@code GroupResult}.
 * <p>
 * This operates on GroupResults that contain an {@link InitialPopulation}.
 *
 * @param stratifierReduceOps holds one {@link StratifierReduceOp} for each stratifier in a group
 */
public record GroupReduceOpInitial(List<StratifierReduceOp<InitialPopulation, InitialIndividual>> stratifierReduceOps)
        implements BiFunction<GroupResult<InitialPopulation, InitialIndividual>, ResourceWithIncludes,
        GroupResult<InitialPopulation, InitialIndividual>> {

    public GroupReduceOpInitial {
        requireNonNull(stratifierReduceOps);
    }

    @Override
    public GroupResult<InitialPopulation, InitialIndividual> apply(GroupResult<InitialPopulation, InitialIndividual> groupResult,
                                                                   ResourceWithIncludes resource) {
        return groupResult.applyResource(stratifierReduceOps, resource, InitialIndividual.INSTANCE);
    }
}
