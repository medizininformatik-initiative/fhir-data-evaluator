package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;

public class GroupEvaluator {
    final String INITIAL_POPULATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-population";
    final String INITIAL_POPULATION_CODE = "initial-population";
    final String INITIAL_POPULATION_LANGUAGE = "text/x-fhir-query";

    private final DataStore dataStore;
    private final FHIRPathEngine fhirPathEngine;

    public GroupEvaluator(DataStore dataStore, FHIRPathEngine fhirPathEngine) {
        this.dataStore = dataStore;
        this.fhirPathEngine = fhirPathEngine;
    }


    public Mono<GroupResult> evaluateGroup(Measure.MeasureGroupComponent group) {
        Flux<Resource> population = dataStore.getPopulation("/" +
                getInitialPopulation(group).getCriteria().getExpressionElement().toString());
        Measure.MeasureGroupPopulationComponent initialPopulationDefinition = group.getPopulation().get(0);

        List<ParsedStratifier> stratElements = group.getStratifier().stream().map(fhirStratifier -> ParsedStratifier.fromFhirStratifier(fhirStratifier, fhirPathEngine)).toList();
        return population.map(resource ->
                        new GroupResult(
                                PopulationsCount.ofInitialPopulation(initialPopulationDefinition).evaluateOnResource(resource),
                                stratElements.stream().map(stratElem -> stratElem.evaluateOnResource(resource, initialPopulationDefinition, fhirPathEngine)).toList()))
                .reduce(GroupResult::merge);
    }

    private Measure.MeasureGroupPopulationComponent getInitialPopulation(Measure.MeasureGroupComponent group) {
        List<Measure.MeasureGroupPopulationComponent> foundInitialPopulations = new LinkedList<>();
        for (Measure.MeasureGroupPopulationComponent populationComponent : group.getPopulation()) {
            List<Coding> codings = populationComponent.getCode().getCoding();
            if (codings.size() != 1)
                throw new IllegalArgumentException("Population in Measure did not contain exactly one Coding");
            Coding coding = codings.get(0);

            if (coding.getSystem().equals(INITIAL_POPULATION_SYSTEM) && coding.getCode().equals(INITIAL_POPULATION_CODE))
                foundInitialPopulations.add(populationComponent);
        }

        if (foundInitialPopulations.size() != 1)
            throw new IllegalArgumentException("Measure did not contain exactly one initial population");
        if (!foundInitialPopulations.get(0).getCriteria().getLanguage().equals(INITIAL_POPULATION_LANGUAGE))
            throw new IllegalArgumentException("Language of Initial Population was not equal to '%s'".formatted(INITIAL_POPULATION_LANGUAGE));
        return foundInitialPopulations.get(0);
    }


}
