package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO java docs
public record ParsedStratifier(HashableCoding coding, Set<StratifierExpression> expressions) {

    public static ParsedStratifier fromFhirStratifier(Measure.MeasureGroupStratifierComponent fhirStratifier, FHIRPathEngine fhirPathEngine) {
        if (fhirStratifier.getCode().getCoding().size() != 1)
            throw new IllegalArgumentException("Stratifier did not contain exactly one coding");
        HashableCoding stratifierCoding = HashableCoding.ofFhirCoding(fhirStratifier.getCode().getCodingFirstRep());

        if (fhirStratifier.hasCriteria() && !fhirStratifier.hasComponent())
            return ParsedStratifier.fromCriteria(stratifierCoding, fhirStratifier, fhirPathEngine);
        else if (fhirStratifier.hasComponent() && !fhirStratifier.hasCriteria())
            return ParsedStratifier.fromComponents(stratifierCoding, fhirStratifier, fhirPathEngine);
        else throw new IllegalArgumentException("Stratifier did not contain either criteria or component exclusively");
    }

    private static ParsedStratifier fromCriteria(HashableCoding stratifierCoding, Measure.MeasureGroupStratifierComponent fhirStratifier, FHIRPathEngine fhirPathEngine) {
        return new ParsedStratifier(stratifierCoding, Set.of(StratifierExpression.fromCriteria(fhirStratifier, fhirPathEngine)));
    }

    private static ParsedStratifier fromComponents(HashableCoding stratifierCoding, Measure.MeasureGroupStratifierComponent fhirStratifier, FHIRPathEngine fhirPathEngine) {
        return new ParsedStratifier(stratifierCoding,
                fhirStratifier.getComponent().stream()
                        .map(component ->
                                StratifierExpression.fromComponent(component, fhirPathEngine))
                        .collect(Collectors.toSet()));
    }

    public StratifierResult evaluateOnResource(Resource resource, Measure.MeasureGroupPopulationComponent initialPopulationCoding, FHIRPathEngine fhirPathEngine) {
        return new StratifierResult(
                Map.of(
                        expressions.stream().map(expression -> expression.evaluate(resource, fhirPathEngine)).collect(Collectors.toSet()),
                        PopulationsCount.ofInitialPopulation(initialPopulationCoding).evaluateOnResource(resource)
                ), coding);
    }


    private record StratifierExpression(HashableCoding definitionCode, ExpressionNode pathExpression) {
        private static final String STRATIFIER_LANGUAGE = "text/fhirpath";
        public static StratifierExpression fromCriteria(Measure.MeasureGroupStratifierComponent fhirStratifier, FHIRPathEngine fhirPathEngine) {
            if (!fhirStratifier.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE))
                throw new IllegalArgumentException("Language of Stratifier was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));

            return new StratifierExpression(
                    HashableCoding.ofFhirCoding(fhirStratifier.getCode().getCodingFirstRep()),
                    fhirPathEngine.parse(fhirStratifier.getCriteria().getExpression()));
        }

        public static StratifierExpression fromComponent(Measure.MeasureGroupStratifierComponentComponent component, FHIRPathEngine fhirPathEngine) {
            if (component.getCode().getCoding().size() != 1)
                throw new IllegalArgumentException("Stratifier component did not contain exactly one coding");
            if (!component.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE))
                throw new IllegalArgumentException("Language of stratifier component was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));

            return new StratifierExpression(
                    HashableCoding.ofFhirCoding(component.getCode().getCodingFirstRep()),
                    fhirPathEngine.parse(component.getCriteria().getExpression()));
        }

        public ComponentKeyPair evaluate(Resource resource, FHIRPathEngine fhirPathEngine) {
            List<Base> found = fhirPathEngine.evaluate(resource, pathExpression);

            if (found.isEmpty())
                return ComponentKeyPair.ofFailedNoValueFound(definitionCode);
            if (found.size() > 1)
                return ComponentKeyPair.ofFailedTooManyValues(definitionCode);
            if (!(found.get(0) instanceof Coding coding))
                return ComponentKeyPair.ofFailedInvalidType(definitionCode);
            if (!coding.hasSystem() || !coding.hasCode())
                return ComponentKeyPair.ofFailedMissingFields(definitionCode);

            HashableCoding valueCode = HashableCoding.ofFhirCoding(coding);
            return new ComponentKeyPair(definitionCode, valueCode);
        }

    }
}
