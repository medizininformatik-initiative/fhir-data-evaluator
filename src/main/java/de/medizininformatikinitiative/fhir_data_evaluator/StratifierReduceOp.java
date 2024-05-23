package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public record StratifierReduceOp(FHIRPathEngine fhirPathEngine,
                                 ParsedStratifier parsedStratifier) implements BiFunction<StratifierResult, Resource, StratifierResult> {
    @Override
    public StratifierResult apply(StratifierResult s, Resource resource) {
        s.mergeResourceResult(evaluateStratifier(resource));
        return s;
    }

    private Set<ComponentKeyPair> evaluateStratifier(Resource resource) {
        return parsedStratifier.componentExpressions().stream().map(e -> evaluateExpression(resource, e)).collect(Collectors.toSet());
    }

    private ComponentKeyPair evaluateExpression(Resource resource, ComponentExpression c) {
        List<Base> found = fhirPathEngine.evaluate(resource, c.pathExpression());

        if (found.isEmpty())
            return ComponentKeyPair.ofFailedNoValueFound(c.coding());
        if (found.size() > 1)
            return ComponentKeyPair.ofFailedTooManyValues(c.coding());
        if (!(found.get(0) instanceof Coding coding))
            return ComponentKeyPair.ofFailedInvalidType(c.coding());
        if (!coding.hasSystem() || !coding.hasCode())
            return ComponentKeyPair.ofFailedMissingFields(c.coding());

        HashableCoding valueCode = HashableCoding.ofFhirCoding(coding);
        return new ComponentKeyPair(c.coding(), valueCode);
    }
}
