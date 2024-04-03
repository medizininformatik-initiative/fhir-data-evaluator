package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Resource;
import reactor.core.publisher.Flux;

public class DataStore {

    /**
     * Executes {@code populationQuery} and returns all resources found with that query.
     *
     * @param populationQuery the fhir search query defining the population
     * @return the resources found with the {@code populationQuery}
     */
    public Flux<Resource> getPopulation(String populationQuery) {
        // TODO
        return Flux.empty();
    }
}
