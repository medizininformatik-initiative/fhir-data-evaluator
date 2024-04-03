package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GroupEvaluator {
    private final DataStore dataStore;
    private final FHIRPathEngine fhirPathEngine;

    public GroupEvaluator(DataStore dataStore, FHIRPathEngine fhirPathEngine) {
        this.dataStore = dataStore;
        this.fhirPathEngine = fhirPathEngine;
    }


    public Mono<GroupResult> evaluateGroup(Measure.MeasureGroupComponent group) {
        Flux<Resource> population = dataStore.getPopulation(
                group.getPopulationFirstRep().getCriteria().getExpressionElement().toString());
        Measure.MeasureGroupPopulationComponent initialPopulationDefinition = group.getPopulation().get(0);

        return population.map(resource ->
                        new GroupResult(
                                PopulationsCount.ofInitialPopulation(initialPopulationDefinition).evaluateOnResource(resource),
                                StratifierEvaluator.evaluateStratifierOnResource(group.getStratifier(), fhirPathEngine, resource, initialPopulationDefinition)))
                .reduce(GroupResult::merge);
    }


}
