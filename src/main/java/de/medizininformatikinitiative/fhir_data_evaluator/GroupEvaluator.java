package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class GroupEvaluator {


    public Mono<List<Optional<StratifierResult>>> evaluateGroup(DataStore dataStore, FHIRPathEngine fhirPathEngine, Measure.MeasureGroupComponent group) {
        List<Measure.MeasureGroupStratifierComponent> stratifier = group.getStratifier();
        Flux<Bundle.BundleEntryComponent> population = dataStore.getPopulation(
                group.getPopulationFirstRep().getCriteria().getExpressionElement().toString());

        return population.map(resource -> stratifier.stream()
                        .map(stratElem -> StratifierEvaluator.evaluateStratElemOnResource(fhirPathEngine, resource, stratElem)).toList())
                .reduce(this::mergeResourceResults);
    }

    private List<Optional<StratifierResult>> mergeResourceResults(List<Optional<StratifierResult>> a, List<Optional<StratifierResult>> b) {
        return IntStream.range(0, a.size()).mapToObj(i -> a.get(i)
                        .flatMap(presentA -> b.get(i).map(presentA::merge)))
                .toList();
    }


}
