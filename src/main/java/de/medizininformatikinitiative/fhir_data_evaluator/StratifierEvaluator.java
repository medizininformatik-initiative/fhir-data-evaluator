package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class StratifierEvaluator {

    public static Optional<StratifierResult> evaluateStratElemOnResource(FHIRPathEngine fhirPathEngine,
                                                                         Bundle.BundleEntryComponent entry,
                                                                         Measure.MeasureGroupStratifierComponent stratifier) {
        return evaluateCoding(fhirPathEngine, entry, stratifier);
    }

    private static Optional<StratifierResult> evaluateCoding(FHIRPathEngine fhirPathEngine,
                                                             Bundle.BundleEntryComponent entry,
                                                             Measure.MeasureGroupStratifierComponent stratifier) {
        return stratifier.hasCriteria() ?
                evaluateStratifierCriteria(fhirPathEngine, entry, stratifier) :
                evaluateStratifierComponents(fhirPathEngine, entry, stratifier);
    }

    private static Optional<StratifierResult> evaluateStratifierCriteria(FHIRPathEngine fhirPathEngine,
                                                                         Bundle.BundleEntryComponent entry,
                                                                         Measure.MeasureGroupStratifierComponent stratifier) {
        try {
            // TODO check for presence? like criteria.hasExpression()
            return Optional.of(CodingResult.ofSingleKey(
                    evaluateExpression(
                            fhirPathEngine, entry, stratifier.getCode().getCodingFirstRep(),
                            stratifier.getCriteria().getExpression())));
        } catch (FHIRException e) {
            // TODO error handling?
            return Optional.empty();
        }
    }

    private static Optional<StratifierResult> evaluateStratifierComponents(FHIRPathEngine fhirPathEngine,
                                                                           Bundle.BundleEntryComponent entry,
                                                                           Measure.MeasureGroupStratifierComponent stratifier) {
        return Optional.of(CodingResult.ofSingleSet(stratifier.getComponent().stream().map(
                component -> evaluateExpression(fhirPathEngine, entry, component.getCode().getCodingFirstRep(),
                        component.getCriteria().getExpression())).collect(Collectors.toSet())));
    }

    private static StratifierCodingKey evaluateExpression(FHIRPathEngine fhirPathEngine, Bundle.BundleEntryComponent entry,
                                                          Coding stratCode, String fhirPath) throws FHIRException {
        List<Base> found = fhirPathEngine.evaluate(entry.getResource(), fhirPath);
        HashableCoding definitionCode = new HashableCoding(stratCode.getSystem(), stratCode.getCode());
        Coding coding = (Coding) found.get(0);
        HashableCoding valueCode = new HashableCoding(coding.getSystem(), coding.getCode());
        return new StratifierCodingKey(definitionCode, valueCode);
    }
}
