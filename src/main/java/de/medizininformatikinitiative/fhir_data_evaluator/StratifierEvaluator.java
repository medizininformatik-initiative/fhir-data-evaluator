package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class StratifierEvaluator {

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
        return stratElem.hasCriteria() ?
                evaluateStratifierCriteria(fhirPathEngine, resource, stratElem, initialPopulationCoding) :
                evaluateStratifierComponents(fhirPathEngine, resource, stratElem, initialPopulationCoding);
    }

    private static StratifierResult evaluateStratifierCriteria(FHIRPathEngine fhirPathEngine,
                                                               Resource resource,
                                                               Measure.MeasureGroupStratifierComponent stratElem,
                                                               Measure.MeasureGroupPopulationComponent initialPopulationCoding) {

        // TODO check for presence? like criteria.hasExpression()
        return evaluateExpression(fhirPathEngine, resource, stratElem.getCode().getCodingFirstRep(), stratElem.getCriteria().getExpression())
                .map(foundValue ->
                        StratifierResult.ofSingleKeyPair(foundValue,
                                PopulationsCount.ofInitialPopulation(initialPopulationCoding).evaluateOnResource(resource),
                                HashableCoding.ofFhirCoding(stratElem.getCode().getCodingFirstRep())))
                .orElse(StratifierResult.empty(HashableCoding.ofFhirCoding(stratElem.getCode().getCodingFirstRep())));
    }

    private static StratifierResult evaluateStratifierComponents(FHIRPathEngine fhirPathEngine,
                                                                 Resource resource,
                                                                 Measure.MeasureGroupStratifierComponent stratElem,
                                                                 Measure.MeasureGroupPopulationComponent initialPopulationCoding) {
        Optional<Set<ComponentKeyPair>> foundValues = stratElem.getComponent().stream()
                .map(component ->
                        evaluateExpression(fhirPathEngine, resource, component.getCode().getCodingFirstRep(),
                                component.getCriteria().getExpression()))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.collectingAndThen(
                        Collectors.toSet(),
                        set -> set.size() == stratElem.getComponent().size() ? Optional.of(set) : Optional.empty()));

        return foundValues.map(set ->
                        StratifierResult.ofSingleSet(set,
                                PopulationsCount.ofInitialPopulation(initialPopulationCoding).evaluateOnResource(resource),
                                HashableCoding.ofFhirCoding(stratElem.getCode().getCodingFirstRep())))
                .orElse(StratifierResult.empty(HashableCoding.ofFhirCoding(stratElem.getCode().getCodingFirstRep())));
    }

    private static Optional<ComponentKeyPair> evaluateExpression(FHIRPathEngine fhirPathEngine, Resource resource,
                                                                 Coding stratCompCode, String fhirPath) throws FHIRException {
        List<Base> found = fhirPathEngine.evaluate(resource, fhirPath);
        if (found.isEmpty())
            return Optional.empty();

        HashableCoding definitionCode = HashableCoding.ofFhirCoding(stratCompCode);
        Coding coding = (Coding) found.get(0); //TODO currently assuming this cast won't fail
        HashableCoding valueCode = HashableCoding.ofFhirCoding(coding);
        return Optional.of(new ComponentKeyPair(definitionCode, valueCode));
    }
}
