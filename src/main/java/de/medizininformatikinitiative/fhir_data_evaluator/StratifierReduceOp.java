package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.Population;
import org.apache.commons.lang3.function.TriFunction;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a reduce operation for one stratifier that consists of one or more components.
 * <p>
 * This operation evaluates each component of the {@code parsedStratifier} and mutates a {@link StratifierResult} to add
 * the evaluated StratumComponents.
 *
 * @param componentExpressions holds one {@link ComponentExpression} for each component of the stratifier
 */
public record StratifierReduceOp<T extends Population<T>>(List<ComponentExpression> componentExpressions)
        implements TriFunction<StratifierResult<T>, Resource, T, StratifierResult<T>> {

    public StratifierReduceOp {
        componentExpressions = List.copyOf(componentExpressions);
    }

    @Override
    public StratifierResult<T> apply(StratifierResult<T> s, Resource resource, T newPopulations) {
        return s.mergeStratumComponents(evaluateStratifier(resource), newPopulations);
    }

    private Set<StratumComponent> evaluateStratifier(Resource resource) {
        return componentExpressions.stream().map(e -> e.evaluate(resource)).collect(Collectors.toSet());
    }
}
