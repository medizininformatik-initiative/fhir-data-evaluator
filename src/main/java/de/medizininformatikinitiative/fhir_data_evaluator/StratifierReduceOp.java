package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Represents a reduce operation for one stratifier that consists of one or more components.
 * <p>
 * This operation evaluates each component of the {@code parsedStratifier} and mutates a {@link StratifierResult} to add
 * the evaluated StratumComponents.
 *
 * @param componentExpressions holds one {@link ComponentExpression} for each component of the stratifier
 * @param populationsTemplate  holds {@link Populations} that defines the populations of the stratifier
 */
public record StratifierReduceOp(List<ComponentExpression> componentExpressions, Populations populationsTemplate,
                                 FHIRPathEngine fhirPathEngine)
        implements BiFunction<StratifierResult, Resource, StratifierResult> {

    public StratifierReduceOp {
        componentExpressions = List.copyOf(componentExpressions);
        requireNonNull(fhirPathEngine);
    }

    @Override
    public StratifierResult apply(StratifierResult s, Resource resource) {
        var newPopulation = populationsTemplate.copy().evaluatePopulations(fhirPathEngine, resource);
        return s.mergeStratumComponents(evaluateStratifier(fhirPathEngine, resource), newPopulation);
    }

    private Set<StratumComponent> evaluateStratifier(FHIRPathEngine fhirPathEngine, Resource resource) {
        return componentExpressions.stream().map(e -> e.evaluate(fhirPathEngine, resource)).collect(Collectors.toSet());
    }
}
