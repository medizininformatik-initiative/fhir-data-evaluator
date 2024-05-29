package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Represents a stratifier with pre-parsed FHIRPaths.
 * <p>
 * In case the stratifier consists of a single criteria instead of components, {@code componentExpressions} will only hold
 * one {@link ComponentExpression}.
 * </p>
 *
 * @param code                 the coding of the stratifier
 * @param componentExpressions componentExpressions of criteria or of each component of the stratifier
 */
public record ParsedStratifier(HashableCoding code, List<ComponentExpression> componentExpressions) {

    public ParsedStratifier {
        requireNonNull(code);
        componentExpressions = List.copyOf(componentExpressions);
    }

    public static ParsedStratifier fromFhirStratifier(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier) {
        if (fhirStratifier.getCode().getCoding().size() != 1) {
            throw new IllegalArgumentException("Stratifier did not contain exactly one coding");
        }

        HashableCoding code = HashableCoding.ofFhirCoding(fhirStratifier.getCode().getCodingFirstRep());

        if (fhirStratifier.hasCriteria() && !fhirStratifier.hasComponent()) {
            return ParsedStratifier.fromCriteria(fhirPathEngine, fhirStratifier, code);
        }

        if (fhirStratifier.hasComponent() && !fhirStratifier.hasCriteria()) {
            return ParsedStratifier.fromComponents(fhirPathEngine, fhirStratifier, code);
        }

        throw new IllegalArgumentException("Stratifier did not contain either criteria or component exclusively");
    }

    private static ParsedStratifier fromCriteria(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier, HashableCoding code) {
        return new ParsedStratifier(code, List.of(ComponentExpression.fromCriteria(fhirPathEngine, fhirStratifier)));
    }

    private static ParsedStratifier fromComponents(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier, HashableCoding code) {
        return new ParsedStratifier(code,
                fhirStratifier.getComponent().stream()
                        .map(component -> ComponentExpression.fromComponent(fhirPathEngine, component))
                        .toList());
    }
}
