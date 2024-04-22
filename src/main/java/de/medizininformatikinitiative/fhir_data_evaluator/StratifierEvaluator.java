package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class StratifierEvaluator {
    private static final String STRATIFIER_LANGUAGE = "text/fhirpath";

    public static List<StratifierResult> evaluateStratifierOnResource(List<Measure.MeasureGroupStratifierComponent> stratifier,
                                                                      FHIRPathEngine fhirPathEngine,
                                                                      Resource resource,
                                                                      Measure.MeasureGroupPopulationComponent initialPopulationCoding) {
        return stratifier.stream().map(stratElem -> evaluateStratElemOnResource(fhirPathEngine, resource, stratElem, initialPopulationCoding))
                .toList();
    }

    private static StratifierResult evaluateStratElemOnResource(FHIRPathEngine fhirPathEngine,
                                                                Resource resource,
                                                                Measure.MeasureGroupStratifierComponent stratElem,
                                                                Measure.MeasureGroupPopulationComponent initialPopulationCoding) {
        if (stratElem.getCode().getCoding().size() != 1)
            throw new IllegalArgumentException("Stratifier did not contain exactly one coding");

        if (stratElem.hasCriteria() && !stratElem.hasComponent())
            return evaluateStratifierCriteria(fhirPathEngine, resource, stratElem, initialPopulationCoding);
        else if (stratElem.hasComponent() && !stratElem.hasCriteria())
            return evaluateStratifierComponents(fhirPathEngine, resource, stratElem, initialPopulationCoding);
        else throw new IllegalArgumentException("Stratifier did not contain either criteria or component exclusively");
    }

    private static StratifierResult evaluateStratifierCriteria(FHIRPathEngine fhirPathEngine,
                                                               Resource resource,
                                                               Measure.MeasureGroupStratifierComponent stratElem,
                                                               Measure.MeasureGroupPopulationComponent initialPopulationCoding) {

        if (!stratElem.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE))
            throw new IllegalArgumentException("Language of Stratifier was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));

        ComponentKeyPair foundValue = evaluateExpression(fhirPathEngine, resource, stratElem.getCode().getCodingFirstRep(), stratElem.getCriteria().getExpression());
        return StratifierResult.ofSingleKeyPair(foundValue,
                PopulationsCount.ofInitialPopulation(initialPopulationCoding).evaluateOnResource(resource),
                HashableCoding.ofFhirCoding(stratElem.getCode().getCodingFirstRep()));
    }

    private static StratifierResult evaluateStratifierComponents(FHIRPathEngine fhirPathEngine,
                                                                 Resource resource,
                                                                 Measure.MeasureGroupStratifierComponent stratElem,
                                                                 Measure.MeasureGroupPopulationComponent initialPopulationCoding) {
        Set<ComponentKeyPair> foundValues = stratElem.getComponent().stream()
                .map(component -> evaluateComponent(fhirPathEngine, resource, component))
                .collect(Collectors.toSet());

        return StratifierResult.ofSingleSet(foundValues,
                PopulationsCount.ofInitialPopulation(initialPopulationCoding).evaluateOnResource(resource),
                HashableCoding.ofFhirCoding(stratElem.getCode().getCodingFirstRep()));
    }

    private static ComponentKeyPair evaluateComponent(FHIRPathEngine fhirPathEngine, Resource resource,
                                                      Measure.MeasureGroupStratifierComponentComponent component) {
        if (component.getCode().getCoding().size() != 1)
            throw new IllegalArgumentException("Stratifier component did not contain exactly one coding");
        if (!component.getCriteria().getLanguage().equals(STRATIFIER_LANGUAGE))
            throw new IllegalArgumentException("Language of stratifier component was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));

        return evaluateExpression(fhirPathEngine, resource, component.getCode().getCodingFirstRep(),
                component.getCriteria().getExpression());
    }

    private static ComponentKeyPair evaluateExpression(FHIRPathEngine fhirPathEngine, Resource resource,
                                                       Coding stratCompCode, String fhirPath) throws FHIRException {
        HashableCoding definitionCode = HashableCoding.ofFhirCoding(stratCompCode);
        List<Base> found = fhirPathEngine.evaluate(resource, fhirPath);

        if (found.isEmpty())
            return ComponentKeyPair.ofFailedNoValueFound(definitionCode);
        if (found.size() > 1)
            return ComponentKeyPair.ofFailedTooManyValues(definitionCode);
        if (!(found.get(0) instanceof Coding coding))
            return ComponentKeyPair.ofFailedInvalidType(definitionCode);
        if(!coding.hasSystem() || !coding.hasCode())
            return ComponentKeyPair.ofFailedMissingFields(definitionCode);

        HashableCoding valueCode = HashableCoding.ofFhirCoding(coding);
        return new ComponentKeyPair(definitionCode, valueCode);
    }
}
