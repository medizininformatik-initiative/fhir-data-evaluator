package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * An operator that appends the data of a {@link Resource} to a {@link GroupResult} producing a new {@code GroupResult}.
 * <p>
 * Applying a {@code GroupReduceOp} to a {@code GroupResult} and a {@code Resource} increments the
 * {@link GroupResult#populations() GroupResult populations} and applies the {@code Resource} to each
 * {@link StratifierResult} in the {@code GroupResult}.
 *
 * @param stratifierReduceOps holds one {@link StratifierReduceOp} for each stratifier in a group
 */
public record GroupReduceOp(List<StratifierReduceOp> stratifierReduceOps)
        implements BiFunction<GroupResult, Resource, GroupResult> {

    public GroupReduceOp {
        requireNonNull(stratifierReduceOps);
    }

    @Override
    public GroupResult apply(GroupResult groupResult, Resource resource) {
        return groupResult.applyResource(stratifierReduceOps, resource);
    }
}
