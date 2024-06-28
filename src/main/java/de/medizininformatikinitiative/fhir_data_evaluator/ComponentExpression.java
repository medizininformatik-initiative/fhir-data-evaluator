package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Holds a {@link HashableCoding} and a pre-parsed FHIRPath {@link ExpressionNode} of a single stratifier component.
 *
 * @param code       the component code
 * @param expression the expression to extract the stratum value
 */
public record ComponentExpression(HashableCoding code, ExpressionNode expression) {

    private static final String STRATIFIER_LANGUAGE = "text/fhirpath";

    public ComponentExpression {
        requireNonNull(code);
        requireNonNull(expression);
    }

    public static ComponentExpression fromCriteria(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier) {
        if (!fhirStratifier.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE)) {
            throw new IllegalArgumentException("Language of Stratifier was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));
        }

        return new ComponentExpression(
                HashableCoding.ofFhirCoding(fhirStratifier.getCode().getCodingFirstRep()),
                fhirPathEngine.parse(fhirStratifier.getCriteria().getExpression()));
    }

    public static ComponentExpression fromComponent(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponentComponent component) {
        if (component.getCode().getCoding().size() != 1) {
            throw new IllegalArgumentException("Stratifier component did not contain exactly one coding");
        }

        if (!component.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE)) {
            throw new IllegalArgumentException("Language of stratifier component was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));
        }

        return new ComponentExpression(
                HashableCoding.ofFhirCoding(component.getCode().getCodingFirstRep()),
                fhirPathEngine.parse(component.getCriteria().getExpression()));
    }

    public StratumComponent evaluate(FHIRPathEngine fhirPathEngine, Resource resource) {
        List<Base> found = fhirPathEngine.evaluate(resource, expression);

        if (found.isEmpty()) {
            return StratumComponent.ofFailedNoValueFound(code);
        }

        if (found.size() > 1) {
            return StratumComponent.ofFailedTooManyValues(code);
        }

        if (found.get(0) instanceof Coding coding) {
            return evaluateCoding(coding);
        }

        if (found.get(0) instanceof Enumeration<?> valueCodeEnumeration) {
            return evaluateCodeEnumeration(valueCodeEnumeration);
        }

        if (found.get(0) instanceof CodeType valueCode) {
            return evaluateCode(valueCode);
        }

        if (found.get(0) instanceof BooleanType bool) {
            return evaluateBoolean(bool);
        }

        return StratumComponent.ofFailedInvalidType(code);
    }

    private StratumComponent evaluateCoding(Coding coding) {

        return (coding.hasSystem() && coding.hasCode()) ?
                new StratumComponent(code, HashableCoding.ofFhirCoding(coding)) :
                StratumComponent.ofFailedMissingFields(code);
    }

    private StratumComponent evaluateCodeEnumeration(Enumeration<?> valueCode) {
        return valueCode.hasCode() ?
                new StratumComponent(code, HashableCoding.ofSingleCodeValue(valueCode.getCode())) :
                StratumComponent.ofFailedMissingFields(code);
    }

    private StratumComponent evaluateCode(CodeType valueCode) {
        return valueCode.hasCode() ?
                new StratumComponent(code, HashableCoding.ofSingleCodeValue(valueCode.getCode())) :
                StratumComponent.ofFailedMissingFields(code);
    }

    private StratumComponent evaluateBoolean(BooleanType bool) {
        return bool.hasValue() ?
                new StratumComponent(code, HashableCoding.ofSingleCodeValue(bool.getValueAsString())) :
                StratumComponent.ofFailedMissingFields(code);
    }
}
