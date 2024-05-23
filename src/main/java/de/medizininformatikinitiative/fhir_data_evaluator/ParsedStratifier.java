package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;

/**
 * Represents a stratifier with pre-parsed FHIRPaths.
 * <p>
 * In case the stratifier consists of a single criteria instead of components, {@code componentExpressions} will only hold
 * one {@link ComponentExpression}.
 * </p>
 *
 * @param coding the coding of the stratifier
 * @param componentExpressions componentExpressions of criteria or of each component of the stratifier
 */
public record ParsedStratifier(HashableCoding coding, List<ComponentExpression> componentExpressions) {

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
        return new ParsedStratifier(stratifierCoding, List.of(ComponentExpression.fromCriteria(fhirStratifier, fhirPathEngine)));
    }

    private static ParsedStratifier fromComponents(HashableCoding stratifierCoding, Measure.MeasureGroupStratifierComponent fhirStratifier, FHIRPathEngine fhirPathEngine) {
        return new ParsedStratifier(stratifierCoding,
                fhirStratifier.getComponent().stream()
                        .map(component ->
                                ComponentExpression.fromComponent(component, fhirPathEngine))
                        .toList());
    }
}
