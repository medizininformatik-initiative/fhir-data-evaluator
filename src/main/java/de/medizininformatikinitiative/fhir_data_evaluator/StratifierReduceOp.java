package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

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
public record StratifierReduceOp(FHIRPathEngine fhirPathEngine,
                                 ParsedStratifier parsedStratifier) implements BiFunction<StratifierResult, Resource, StratifierResult> {
    @Override
    public StratifierResult apply(StratifierResult s, Resource resource) {
        s.mergeResourceResult(evaluateStratifier(resource));
        return s;
    }

    private Set<StratumComponent> evaluateStratifier(Resource resource) {
        return parsedStratifier.componentExpressions().stream().map(e -> evaluateExpression(resource, e)).collect(Collectors.toSet());
    }

    private StratumComponent evaluateExpression(Resource resource, ComponentExpression c) {
        List<Base> found = fhirPathEngine.evaluate(resource, c.pathExpression());

        if (found.isEmpty())
            return StratumComponent.ofFailedNoValueFound(c.coding());
        if (found.size() > 1)
            return StratumComponent.ofFailedTooManyValues(c.coding());
        if (!(found.get(0) instanceof Coding coding))
            return StratumComponent.ofFailedInvalidType(c.coding());
        if (!coding.hasSystem() || !coding.hasCode())
            return StratumComponent.ofFailedMissingFields(c.coding());

        HashableCoding valueCode = HashableCoding.ofFhirCoding(coding);
        return new StratumComponent(c.coding(), valueCode);
    }
}
