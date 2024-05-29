package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Represents a reduce operation for one stratifier that consists of one or more components.
 * <p>
 * This operation evaluates each component of the {@code parsedStratifier} and mutates a {@link StratifierResult} to add
 * the evaluated StratumComponents.
 */
public record StratifierReduceOp(List<ComponentExpression> componentExpressions)
        implements BiFunction<StratifierResult, Resource, StratifierResult> {

    public StratifierReduceOp {
        componentExpressions = List.copyOf(componentExpressions);
    }

    @Override
    public StratifierResult apply(StratifierResult s, Resource resource) {
        return s.mergeStratumComponents(evaluateStratifier(resource));
    }

    private Set<StratumComponent> evaluateStratifier(Resource resource) {
        return componentExpressions.stream().map(e -> e.evaluate(resource)).collect(Collectors.toSet());
    }
}
