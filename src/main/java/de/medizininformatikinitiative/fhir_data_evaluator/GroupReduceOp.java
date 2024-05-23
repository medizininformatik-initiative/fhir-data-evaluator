package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.function.BiFunction;

public record GroupReduceOp(
        List<StratifierReduceOp> stratifierOperations) implements BiFunction<GroupResult, Resource, GroupResult> {

    @Override
    public GroupResult apply(GroupResult groupResult, Resource resource) {
        return groupResult.applyResource(stratifierOperations, resource);
    }
}
