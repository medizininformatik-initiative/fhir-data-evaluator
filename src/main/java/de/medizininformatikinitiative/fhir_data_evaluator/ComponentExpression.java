package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.fhirpath.IFhirPath;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.ExpressionNode;
import org.hl7.fhir.r4.model.Measure;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Holds a {@link HashableCoding} and a pre-parsed FHIRPath {@link ExpressionNode} of a single stratifier component.
 *
 * @param code           the component code
 * @param expression     the expression to extract the stratum value
 */
public record ComponentExpression(HashableCoding code, IFhirPath.IParsedExpression expression) {

    private static final String STRATIFIER_LANGUAGE = "text/fhirpath";

    public ComponentExpression {
        requireNonNull(code);
        requireNonNull(expression);
    }

    public static ComponentExpression fromCriteria(IFhirPath fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier) throws Exception {
        if (!fhirStratifier.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE)) {
            throw new IllegalArgumentException("Language of Stratifier was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));
        }

        return new ComponentExpression(
                HashableCoding.ofFhirCoding(fhirStratifier.getCode().getCodingFirstRep()),
                fhirPathEngine.parse(fhirStratifier.getCriteria().getExpression()));
    }

    public static ComponentExpression fromComponent(IFhirPath fhirPathEngine, Measure.MeasureGroupStratifierComponentComponent component) {
        if (component.getCode().getCoding().size() != 1) {
            throw new IllegalArgumentException("Stratifier component did not contain exactly one coding");
        }

        if (!component.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE)) {
            throw new IllegalArgumentException("Language of stratifier component was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));
        }

        try {
            return new ComponentExpression(
                    HashableCoding.ofFhirCoding(component.getCode().getCodingFirstRep()),
                    fhirPathEngine.parse(component.getCriteria().getExpression()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public StratumComponent evaluate(ResourceWithIncludes resource) {
        List<Base> found = resource.fhirPathEngine().evaluate(resource.mainResource(), expression, Base.class);

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
