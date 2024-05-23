package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.ExpressionNode;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

/**
 * Holds a {@link HashableCoding} and a pre-parsed FHIRPath {@link ExpressionNode} of a single stratifier component.
 */
public record ComponentExpression(HashableCoding coding, ExpressionNode pathExpression) {
    private static final String STRATIFIER_LANGUAGE = "text/fhirpath";

    public static ComponentExpression fromCriteria(Measure.MeasureGroupStratifierComponent fhirStratifier, FHIRPathEngine fhirPathEngine) {
        if (!fhirStratifier.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE))
            throw new IllegalArgumentException("Language of Stratifier was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));

        return new ComponentExpression(
                HashableCoding.ofFhirCoding(fhirStratifier.getCode().getCodingFirstRep()),
                fhirPathEngine.parse(fhirStratifier.getCriteria().getExpression()));
    }

    public static ComponentExpression fromComponent(Measure.MeasureGroupStratifierComponentComponent component, FHIRPathEngine fhirPathEngine) {
        if (component.getCode().getCoding().size() != 1)
            throw new IllegalArgumentException("Stratifier component did not contain exactly one coding");
        if (!component.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE))
            throw new IllegalArgumentException("Language of stratifier component was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));

        return new ComponentExpression(
                HashableCoding.ofFhirCoding(component.getCode().getCodingFirstRep()),
                fhirPathEngine.parse(component.getCriteria().getExpression()));
    }

}
