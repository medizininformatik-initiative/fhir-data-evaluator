package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.Population;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.Individual;
import org.apache.commons.lang3.function.TriFunction;

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
public record StratifierReduceOp<T extends Population<T, I>, I extends Individual<T>>(
        List<ComponentExpression> componentExpressions)
        implements TriFunction<StratifierResult<T, I>, ResourceWithIncludes, I, StratifierResult<T, I>> {

    public StratifierReduceOp {
        componentExpressions = List.copyOf(componentExpressions);
    }

    @Override
    public StratifierResult<T, I> apply(StratifierResult<T, I> s, ResourceWithIncludes resource, I incrementIndividual) {
        return s.mergeStratumComponents(evaluateStratifier(resource), incrementIndividual);
    }

    private Set<StratumComponent> evaluateStratifier(ResourceWithIncludes resource) {
        return componentExpressions.stream().map(e -> e.evaluate(resource)).collect(Collectors.toSet());
    }
}
